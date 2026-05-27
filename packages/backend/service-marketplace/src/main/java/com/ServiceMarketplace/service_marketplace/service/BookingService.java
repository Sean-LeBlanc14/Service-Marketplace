package com.ServiceMarketplace.service_marketplace.service;

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
import com.ServiceMarketplace.service_marketplace.model.User;
import com.ServiceMarketplace.service_marketplace.repository.BookingRepository;
import com.ServiceMarketplace.service_marketplace.repository.ServiceRepository;
import com.ServiceMarketplace.service_marketplace.repository.UserRepository;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final EmailService emailService;

    public BookingService(BookingRepository bookingRepository, ServiceRepository serviceRepository, UserRepository userRepository, PaymentService paymentService, EmailService emailService) {
        this.bookingRepository = bookingRepository;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
        this.emailService = emailService;
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

        emailService.sendProviderBookingNotificationEmail(
            provider.getEmail(),
            provider.getFirstName(),
            customerName,
            service.getTitle(),
            request.getProposedPrice(),
            service.getPriceUnit(),
            request.getScheduledAt()
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

        if (booking.getStatus() != BookingStatus.AWAITING_PROVIDER_CONFIRMATION) {
            throw new BookingStateException("Booking is not awaiting provider confirmation");
        }

        com.ServiceMarketplace.service_marketplace.model.Service service = serviceRepository
            .findById(booking.getServiceId())
            .orElseThrow(() -> new ResourceNotFoundException("Service", booking.getServiceId()));

        if (request.getConfirmedPrice().compareTo(service.getPriceMin()) < 0 ||
            request.getConfirmedPrice().compareTo(service.getPriceMax()) > 0) {
            throw new InvalidPriceException(
                "Confirmed price must be between " + service.getPriceMin() + " and " + service.getPriceMax()
            );
        }

        PaymentIntentResult paymentResult = paymentService.createAndConfirmPaymentIntent(
            request.getConfirmedPrice(),
            booking.getStripeCustomerId(),
            booking.getServiceId(),
            booking.getCustomerId(),
            provider.getStripeAccountId()
        );

        booking.setAgreedPrice(request.getConfirmedPrice());
        booking.setStripePaymentIntentId(paymentResult.getPaymentIntentId());
        booking.setStatus(BookingStatus.PENDING_PAYMENT);

        return toBookingResponse(bookingRepository.save(booking));
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

        if (booking.getStatus() != BookingStatus.AWAITING_PROVIDER_CONFIRMATION) {
            throw new BookingStateException("Only bookings awaiting confirmation can be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        paymentService.cleanupStripeCustomer(booking.getStripeCustomerId());

        return toBookingResponse(booking);
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
