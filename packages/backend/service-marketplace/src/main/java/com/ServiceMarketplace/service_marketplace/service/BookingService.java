package com.ServiceMarketplace.service_marketplace.service;

import org.springframework.stereotype.Service;

import com.ServiceMarketplace.service_marketplace.dto.CreateBookingRequest;
import com.ServiceMarketplace.service_marketplace.exception.InvalidPriceException;
import com.ServiceMarketplace.service_marketplace.exception.ResourceNotFoundException;
import com.ServiceMarketplace.service_marketplace.model.Booking;
import com.ServiceMarketplace.service_marketplace.model.BookingStatus;
import com.ServiceMarketplace.service_marketplace.repository.BookingRepository;
import com.ServiceMarketplace.service_marketplace.repository.ServiceRepository;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;

    public BookingService(BookingRepository bookingRepository, ServiceRepository serviceRepository) {
        this.bookingRepository = bookingRepository;
        this.serviceRepository = serviceRepository;
    }

    public Booking createBooking(CreateBookingRequest request) {
        com.ServiceMarketplace.service_marketplace.model.Service service = serviceRepository
            .findById(request.serviceId())
            .orElseThrow(() -> new ResourceNotFoundException("Service", request.serviceId()));

        if (request.agreedPrice().compareTo(service.getPriceMin()) < 0 ||
            request.agreedPrice().compareTo(service.getPriceMax()) > 0) {
            throw new InvalidPriceException(
                "Agreed price must be between " + service.getPriceMin() + " and " + service.getPriceMax()
            );
        }

        Booking booking = new Booking();
        booking.setServiceId(request.serviceId());
        booking.setCustomerId(request.customerId());
        booking.setProviderId(service.getUserId());
        booking.setServiceTitle(service.getTitle());
        booking.setAgreedPrice(request.agreedPrice());
        booking.setPriceUnit(service.getPriceUnit());
        booking.setScheduledAt(request.scheduledAt());
        booking.setStatus(BookingStatus.PENDING_PAYMENT);

        return bookingRepository.save(booking);
    }
}