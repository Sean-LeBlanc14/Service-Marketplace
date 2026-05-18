package com.ServiceMarketplace.service_marketplace.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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

    public Booking createBooking(CreateBookingRequest request, UserDetails userDetails) {
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

        return bookingRepository.save(booking);
    }
}