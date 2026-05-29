package com.ServiceMarketplace.service_marketplace.service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ServiceMarketplace.service_marketplace.dto.BookingResponse;
import com.ServiceMarketplace.service_marketplace.dto.CreateBookingRequest;
import com.ServiceMarketplace.service_marketplace.dto.CreateBookingResponse;
import com.ServiceMarketplace.service_marketplace.dto.PaymentIntentResult;
import com.ServiceMarketplace.service_marketplace.dto.SubmitReviewRequest;
import com.ServiceMarketplace.service_marketplace.exception.InvalidBookingReviewException;
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

    public BookingService(BookingRepository bookingRepository, ServiceRepository serviceRepository, UserRepository userRepository, PaymentService paymentService) {
        this.bookingRepository = bookingRepository;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
    }

    public CreateBookingResponse createBooking(CreateBookingRequest request, UserDetails userDetails) {
        var customer = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        com.ServiceMarketplace.service_marketplace.model.Service service = serviceRepository
            .findById(request.getServiceId())
            .orElseThrow(() -> new ResourceNotFoundException("Service", request.getServiceId()));

        if (request.getAgreedPrice().compareTo(service.getPriceMin()) < 0 ||
            request.getAgreedPrice().compareTo(service.getPriceMax()) > 0) {
            throw new InvalidPriceException(
                "Agreed price must be between " + service.getPriceMin() + " and " + service.getPriceMax()
            );
        }

        User provider = userRepository.findById(service.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("Provider", service.getUserId()));
        String providerStripeAccountId = provider.getStripeAccountId();

        PaymentIntentResult paymentIntentResult = paymentService.createPaymentIntent(
            request.getAgreedPrice(),
            request.getServiceId(),
            customer.getId(),
            providerStripeAccountId
        );

        Booking booking = new Booking();
        booking.setServiceId(request.getServiceId());
        booking.setCustomerId(customer.getId());
        booking.setProviderId(service.getUserId());
        booking.setServiceTitle(service.getTitle());
        booking.setAgreedPrice(request.getAgreedPrice());
        booking.setPriceUnit(service.getPriceUnit());
        booking.setScheduledAt(request.getScheduledAt());
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setStripePaymentIntentId(paymentIntentResult.getPaymentIntentId());

        Booking saved = bookingRepository.save(booking);

        return new CreateBookingResponse(toBookingResponse(saved), paymentIntentResult.getClientSecret());
    }

    public List<BookingResponse> getCustomerBookings(UserDetails userDetails) {
        var customer = getCurrentUser(userDetails);

        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())
            .stream()
            .map(this::toBookingResponse)
            .collect(Collectors.toList());
    }

    public BookingResponse submitReview(String bookingId, SubmitReviewRequest request, UserDetails userDetails) {
        var customer = getCurrentUser(userDetails);
        var booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (!customer.getId().equals(booking.getCustomerId())) {
            throw new AccessDeniedException("You can only review your own bookings.");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidBookingReviewException("You can only review confirmed bookings.");
        }

        booking.setRating(request.getRating());
        booking.setReview(clean(request.getReview()));
        booking.setReviewedAt(Instant.now());

        return toBookingResponse(bookingRepository.save(booking));
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

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
