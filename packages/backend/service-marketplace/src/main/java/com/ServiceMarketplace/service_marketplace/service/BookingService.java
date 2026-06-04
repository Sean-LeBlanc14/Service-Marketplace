package com.ServiceMarketplace.service_marketplace.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ServiceMarketplace.service_marketplace.dto.BookingResponse;
import com.ServiceMarketplace.service_marketplace.dto.ConfirmBookingRequest;
import com.ServiceMarketplace.service_marketplace.dto.CreateBookingRequest;
import com.ServiceMarketplace.service_marketplace.dto.CreateBookingResponse;
import com.ServiceMarketplace.service_marketplace.dto.PaymentIntentResult;
import com.ServiceMarketplace.service_marketplace.dto.SetupIntentResult;
import com.ServiceMarketplace.service_marketplace.dto.SubmitReviewRequest;
import com.ServiceMarketplace.service_marketplace.exception.BookingStateException;
import com.ServiceMarketplace.service_marketplace.exception.InvalidBookingReviewException;
import com.ServiceMarketplace.service_marketplace.exception.InvalidPriceException;
import com.ServiceMarketplace.service_marketplace.exception.ResourceNotFoundException;
import com.ServiceMarketplace.service_marketplace.model.Booking;
import com.ServiceMarketplace.service_marketplace.model.BookingStatus;
import com.ServiceMarketplace.service_marketplace.model.BookingTokenAction;
import com.ServiceMarketplace.service_marketplace.model.NotificationType;
import com.ServiceMarketplace.service_marketplace.model.User;
import com.ServiceMarketplace.service_marketplace.repository.BookingRepository;
import com.ServiceMarketplace.service_marketplace.repository.ServiceRepository;
import com.ServiceMarketplace.service_marketplace.repository.UserRepository;
import com.ServiceMarketplace.service_marketplace.service.BookingTokenService.TokenPair;
import com.ServiceMarketplace.service_marketplace.service.BookingTokenService.TokenResult;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final EmailService emailService;
    private final BookingTokenService bookingTokenService;
    private final NotificationService notificationService;

    public BookingService(BookingRepository bookingRepository, ServiceRepository serviceRepository,
            UserRepository userRepository, PaymentService paymentService, EmailService emailService,
            BookingTokenService bookingTokenService, NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
        this.emailService = emailService;
        this.bookingTokenService = bookingTokenService;
        this.notificationService = notificationService;
    }

    public CreateBookingResponse createBooking(CreateBookingRequest request, UserDetails userDetails) {
        User customer = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        com.ServiceMarketplace.service_marketplace.model.Service service = serviceRepository
            .findById(request.getServiceId())
            .orElseThrow(() -> new ResourceNotFoundException("Service", request.getServiceId()));

        if (request.getProposedPrice().compareTo(service.getPriceMin()) < 0 ||
            request.getProposedPrice().compareTo(service.getPriceMax()) > 0) {
            throw new InvalidPriceException(
                "Proposed price must be between " + service.getPriceMin() + " and " + service.getPriceMax()
            );
        }

        User provider = userRepository.findById(service.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("Provider", service.getUserId()));

        String customerName = customer.getFirstName() + " " + customer.getLastName();
        SetupIntentResult setupResult = paymentService.createSetupIntent(customer.getEmail(), customerName);

        Booking booking = new Booking();
        booking.setServiceId(request.getServiceId());
        booking.setCustomerId(customer.getId());
        booking.setProviderId(service.getUserId());
        booking.setServiceTitle(service.getTitle());
        booking.setAgreedPrice(request.getProposedPrice());
        booking.setPriceUnit(service.getPriceUnit());
        booking.setScheduledAt(request.getScheduledAt());
        booking.setStatus(BookingStatus.AWAITING_PROVIDER_CONFIRMATION);
        booking.setStripeSetupIntentId(setupResult.getSetupIntentId());
        booking.setStripeCustomerId(setupResult.getStripeCustomerId());

        Booking saved = bookingRepository.save(booking);

        emailService.sendBookingRequestedCustomerEmail(
            customer.getEmail(),
            customer.getFirstName(),
            service.getTitle(),
            request.getProposedPrice(),
            service.getPriceUnit(),
            request.getScheduledAt(),
            saved.getId()
        );

        TokenPair tokenPair = bookingTokenService.generateTokenPair(saved.getId());

        emailService.sendProviderBookingNotificationEmail(
            provider.getEmail(),
            provider.getFirstName(),
            customerName,
            service.getTitle(),
            request.getProposedPrice(),
            service.getPriceUnit(),
            request.getScheduledAt(),
            tokenPair.confirmUrl(),
            tokenPair.cancelUrl()
        );

        notificationService.send(provider.getId(), NotificationType.BOOKING_REQUESTED,
            "New Booking Request",
            customerName + " has requested to book " + service.getTitle(),
            saved.getId());

        return new CreateBookingResponse(toBookingResponse(saved), setupResult.getSetupClientSecret());
    }

    public BookingResponse confirmBooking(String bookingId, ConfirmBookingRequest request, UserDetails userDetails) {
        User provider = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (!booking.getProviderId().equals(provider.getId())) {
            throw new AccessDeniedException("You are not authorized to confirm this booking");
        }

        return doConfirmBooking(booking, request.getConfirmedPrice(), provider);
    }

    public BookingResponse cancelBooking(String bookingId, UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        boolean isCustomer = booking.getCustomerId().equals(user.getId());
        boolean isProvider = booking.getProviderId().equals(user.getId());

        if (!isCustomer && !isProvider) {
            throw new AccessDeniedException("You are not authorized to cancel this booking");
        }

        return doCancelBooking(booking, isCustomer);
    }

    public BookingTokenAction processTokenAction(String token) {
        TokenResult result = bookingTokenService.validateAndConsume(token);

        Booking booking = bookingRepository.findById(result.bookingId())
            .orElseThrow(() -> new ResourceNotFoundException("Booking", result.bookingId()));

        if (result.action() == BookingTokenAction.CONFIRM) {
            User provider = userRepository.findById(booking.getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider", booking.getProviderId()));
            doConfirmBooking(booking, booking.getAgreedPrice(), provider);
        } else {
            doCancelBooking(booking, false);
        }

        return result.action();
    }

    private BookingResponse doConfirmBooking(Booking booking, BigDecimal price, User provider) {
        if (booking.getStatus() != BookingStatus.AWAITING_PROVIDER_CONFIRMATION) {
            throw new BookingStateException("Booking is not awaiting provider confirmation");
        }

        com.ServiceMarketplace.service_marketplace.model.Service service = serviceRepository
            .findById(booking.getServiceId())
            .orElseThrow(() -> new ResourceNotFoundException("Service", booking.getServiceId()));

        if (price.compareTo(service.getPriceMin()) < 0 || price.compareTo(service.getPriceMax()) > 0) {
            throw new InvalidPriceException(
                "Confirmed price must be between " + service.getPriceMin() + " and " + service.getPriceMax()
            );
        }

        PaymentIntentResult paymentResult = paymentService.createAndConfirmPaymentIntent(
            price,
            booking.getStripeCustomerId(),
            booking.getServiceId(),
            booking.getCustomerId(),
            provider.getStripeAccountId()
        );

        booking.setAgreedPrice(price);
        booking.setStripePaymentIntentId(paymentResult.getPaymentIntentId());
        booking.setStatus(BookingStatus.PENDING_PAYMENT);

        Booking confirmedBooking = bookingRepository.save(booking);

        userRepository.findById(confirmedBooking.getCustomerId()).ifPresent(customer ->
            notificationService.send(customer.getId(), NotificationType.BOOKING_CONFIRMED,
                "Booking Confirmed",
                provider.getFirstName() + " confirmed your booking for " + confirmedBooking.getServiceTitle(),
                confirmedBooking.getId()));

        return toBookingResponse(confirmedBooking);
    }

    private BookingResponse doCancelBooking(Booking booking, boolean cancelledByCustomer) {
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingStateException("Booking is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        paymentService.cleanupStripeCustomer(booking.getStripeCustomerId());

        if (cancelledByCustomer) {
            userRepository.findById(booking.getProviderId()).ifPresent(provider -> {
                String customerName = userRepository.findById(booking.getCustomerId())
                    .map(c -> c.getFirstName() + " " + c.getLastName())
                    .orElse("A customer");
                emailService.sendBookingCancelledProviderEmail(
                    provider.getEmail(),
                    provider.getFirstName(),
                    customerName,
                    booking.getServiceTitle(),
                    booking.getScheduledAt(),
                    booking.getId()
                );
                notificationService.send(provider.getId(), NotificationType.BOOKING_CANCELLED,
                    "Booking Cancelled",
                    customerName + " cancelled their booking for " + booking.getServiceTitle(),
                    booking.getId());
            });
        } else {
            userRepository.findById(booking.getCustomerId()).ifPresent(customer -> {
                String providerName = userRepository.findById(booking.getProviderId())
                    .map(p -> p.getFirstName() + " " + p.getLastName())
                    .orElse("The provider");
                emailService.sendBookingCancelledCustomerEmail(
                    customer.getEmail(),
                    customer.getFirstName(),
                    providerName,
                    booking.getServiceTitle(),
                    booking.getScheduledAt(),
                    booking.getId()
                );
                notificationService.send(customer.getId(), NotificationType.BOOKING_CANCELLED,
                    "Booking Cancelled",
                    providerName + " cancelled your booking for " + booking.getServiceTitle(),
                    booking.getId());
            });
        }

        return toBookingResponse(booking);
    }

    //Gets all pending bookings for a provider
    public List<BookingResponse> getUserBookingRequests(UserDetails userDetails){

        var user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow(() -> new UsernameNotFoundException("user not found"));

        List<Booking> bookingRequests = bookingRepository.findByProviderIdAndStatus(user.getId(), BookingStatus.AWAITING_PROVIDER_CONFIRMATION);

        List<BookingResponse> response = bookingRequests.stream()
            .map(this::toBookingResponse)
            .toList();
        
        return response;
        
    }   

    //Gets all completed services/bookings for a provider
    public List<BookingResponse> getUserCompletedBookings(UserDetails userDetails){
        
        var user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow(() -> new UsernameNotFoundException("user not found"));
        
        List<Booking> bookings = bookingRepository.findByProviderIdAndStatus(user.getId(), BookingStatus.COMPLETED);

        List<BookingResponse> response = bookings.stream().map(this::toBookingResponse).toList();

        return response;
    }

    //Gets all upcoming bookings for a provider
    public List<BookingResponse> getUserScheduledBookings(UserDetails userDetails){
        var user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Booking> bookings = bookingRepository.findByProviderIdAndStatus(user.getId(), BookingStatus.CONFIRMED);

        List<BookingResponse> response = bookings.stream().map(this::toBookingResponse).toList();

        return response;
    }

    public List<BookingResponse> getCustomerBookings(UserDetails userDetails) {
        var customer = getCurrentUser(userDetails);
        var bookings = bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId());
        var usersById = getUsersById(bookings);

        return bookings.stream()
            .map(booking -> toBookingResponse(booking, usersById))
            .collect(Collectors.toList());
    }

    public BookingResponse submitReview(String bookingId, SubmitReviewRequest request, UserDetails userDetails) {
        var customer = getCurrentUser(userDetails);
        var booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (!customer.getId().equals(booking.getCustomerId())) {
            throw new AccessDeniedException("You can only review your own bookings.");
        }

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new InvalidBookingReviewException("You can only review completed bookings.");
        }

        if (booking.getRating() != null) {
            throw new InvalidBookingReviewException("This booking has already been reviewed.");
        }

        booking.setRating(request.getRating());
        booking.setReview(clean(request.getReview()));
        booking.setReviewerName(getUserDisplayName(customer));
        booking.setReviewedAt(Instant.now());

        Booking reviewed = bookingRepository.save(booking);

        notificationService.send(reviewed.getProviderId(), NotificationType.REVIEW_RECEIVED,
            "New Review Received",
            getUserDisplayName(customer) + " left a " + request.getRating() + "-star review for " + reviewed.getServiceTitle(),
            reviewed.getId());

        return toBookingResponse(reviewed);
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private BookingResponse toBookingResponse(Booking booking) {
        return new BookingResponse(
            booking.getId(),
            booking.getServiceId(),
            booking.getServiceTitle(),
            booking.getCustomerId(),
            booking.getProviderId(),
            getUserDisplayName(booking.getCustomerId()),
            getUserDisplayName(booking.getProviderId()),
            getReviewerName(booking),
            booking.getAgreedPrice(),
            booking.getPriceUnit(),
            booking.getScheduledAt(),
            booking.getStatus(),
            booking.getRating(),
            booking.getReview(),
            booking.getReviewedAt(),
            booking.getCreatedAt()
        );
    }

    private BookingResponse toBookingResponse(Booking booking, Map<String, User> usersById) {
        return new BookingResponse(
            booking.getId(),
            booking.getServiceId(),
            booking.getServiceTitle(),
            booking.getCustomerId(),
            booking.getProviderId(),
            getUserDisplayName(booking.getCustomerId(), usersById),
            getUserDisplayName(booking.getProviderId(), usersById),
            getReviewerName(booking, usersById),
            booking.getAgreedPrice(),
            booking.getPriceUnit(),
            booking.getScheduledAt(),
            booking.getStatus(),
            booking.getRating(),
            booking.getReview(),
            booking.getReviewedAt(),
            booking.getCreatedAt()
        );
    }

    private Map<String, User> getUsersById(List<Booking> bookings) {
        Set<String> userIds = new HashSet<>();

        for (Booking booking : bookings) {
            addUserId(userIds, booking.getCustomerId());
            addUserId(userIds, booking.getProviderId());
        }

        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userRepository.findAllById(userIds)
            .stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private void addUserId(Set<String> userIds, String userId) {
        String cleanUserId = clean(userId);

        if (!cleanUserId.isBlank()) {
            userIds.add(cleanUserId);
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String getUserDisplayName(String userId) {
        String cleanUserId = clean(userId);

        if (cleanUserId.isBlank()) {
            return "";
        }

        var user = userRepository.findById(cleanUserId);

        if (user.isEmpty()) {
            return cleanUserId;
        }

        User foundUser = user.get();
        return getUserDisplayName(foundUser);
    }

    private String getUserDisplayName(String userId, Map<String, User> usersById) {
        String cleanUserId = clean(userId);

        if (cleanUserId.isBlank()) {
            return "";
        }

        User user = usersById.get(cleanUserId);
        return user == null ? cleanUserId : getUserDisplayName(user);
    }

    private String getUserDisplayName(User user) {
        String fullName = (clean(user.getFirstName()) + " " + clean(user.getLastName())).trim();

        if (!fullName.isBlank()) {
            return fullName;
        }

        String email = clean(user.getEmail());
        return email.isBlank() ? clean(user.getId()) : email;
    }

    private String getReviewerName(Booking booking) {
        String reviewerName = clean(booking.getReviewerName());
        return reviewerName.isBlank() ? getUserDisplayName(booking.getCustomerId()) : reviewerName;
    }

    private String getReviewerName(Booking booking, Map<String, User> usersById) {
        String reviewerName = clean(booking.getReviewerName());
        return reviewerName.isBlank() ? getUserDisplayName(booking.getCustomerId(), usersById) : reviewerName;
    }
}
