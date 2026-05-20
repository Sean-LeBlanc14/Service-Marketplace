package com.ServiceMarketplace.service_marketplace.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ServiceMarketplace.service_marketplace.dto.BookingResponse;
import com.ServiceMarketplace.service_marketplace.dto.CreateBookingRequest;
import com.ServiceMarketplace.service_marketplace.exception.InvalidPriceException;
import com.ServiceMarketplace.service_marketplace.exception.ResourceNotFoundException;
import com.ServiceMarketplace.service_marketplace.model.Booking;
import com.ServiceMarketplace.service_marketplace.model.BookingStatus;
import com.ServiceMarketplace.service_marketplace.repository.BookingRepository;
import com.ServiceMarketplace.service_marketplace.repository.ServiceRepository;
import com.ServiceMarketplace.service_marketplace.repository.UserRepository;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository, ServiceRepository serviceRepository, UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
    }

    public BookingResponse createBooking(CreateBookingRequest request, UserDetails userDetails) {
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

        Booking booking = new Booking();
        booking.setServiceId(request.getServiceId());
        booking.setCustomerId(customer.getId());
        booking.setProviderId(service.getUserId());
        booking.setServiceTitle(service.getTitle());
        booking.setAgreedPrice(request.getAgreedPrice());
        booking.setPriceUnit(service.getPriceUnit());
        booking.setScheduledAt(request.getScheduledAt());
        booking.setStatus(BookingStatus.PENDING_PAYMENT);

        Booking saved = bookingRepository.save(booking);
        return toBookingResponse(saved);
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