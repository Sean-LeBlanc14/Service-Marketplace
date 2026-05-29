package com.ServiceMarketplace.service_marketplace.service;

import java.math.BigDecimal;
import java.util.List;

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
import com.ServiceMarketplace.service_marketplace.exception.BookingStateException;
import com.ServiceMarketplace.service_marketplace.exception.InvalidPriceException;
import com.ServiceMarketplace.service_marketplace.exception.ResourceNotFoundException;
import com.ServiceMarketplace.service_marketplace.model.Booking;
import com.ServiceMarketplace.service_marketplace.model.BookingStatus;
import com.ServiceMarketplace.service_marketplace.model.BookingTokenAction;
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

    public BookingService(BookingRepository bookingRepository, ServiceRepository serviceRepository,
            UserRepository userRepository, PaymentService paymentService, EmailService emailService,
            BookingTokenService bookingTokenService) {
        this.bookingRepository = bookingRepository;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
        this.emailService = emailService;
        this.bookingTokenService = bookingTokenService;
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

        return doCancelBooking(booking);
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
            doCancelBooking(booking);
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

        return toBookingResponse(bookingRepository.save(booking));
    }

    private BookingResponse doCancelBooking(Booking booking) {
        if (booking.getStatus() != BookingStatus.AWAITING_PROVIDER_CONFIRMATION 
            && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BookingStateException("Only bookings awaiting confirmation can be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        paymentService.cleanupStripeCustomer(booking.getStripeCustomerId());

        return toBookingResponse(booking);
    }

    public List<BookingResponse> getUserBookingRequests(UserDetails userDetails){

        var user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow(() -> new UsernameNotFoundException("user not found"));

        List<Booking> bookingRequests = bookingRepository.findByProviderIdAndStatus(user.getId(), BookingStatus.AWAITING_PROVIDER_CONFIRMATION);

        List<BookingResponse> response = bookingRequests.stream()
            .map(this::toBookingResponse)
            .toList();
        
        return response;
        
    }

    public List<BookingResponse> getUserCompletedBookings(UserDetails userDetails){
        
        var user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow(() -> new UsernameNotFoundException("user not found"));
        
        List<Booking> bookings = bookingRepository.findByProviderIdAndStatus(user.getId(), BookingStatus.COMPLETED);

        List<BookingResponse> response = bookings.stream().map(this::toBookingResponse).toList();

        return response;
    }

    public List<BookingResponse> getUserScheduledBookings(UserDetails userDetails){
        var user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Booking> bookings = bookingRepository.findByProviderIdAndStatus(user.getId(), BookingStatus.CONFIRMED);

        List<BookingResponse> response = bookings.stream().map(this::toBookingResponse).toList();

        return response;
    }

    private BookingResponse toBookingResponse(Booking booking) {
        return new BookingResponse(
            booking.getId(),
            booking.getServiceId(),
            booking.getServiceTitle(),
            booking.getCustomerId(),
            booking.getProviderId(),
            booking.getAgreedPrice(),
            booking.getPriceUnit(),
            booking.getScheduledAt(),
            booking.getStatus(),
            booking.getCreatedAt()
        );
    }
}
