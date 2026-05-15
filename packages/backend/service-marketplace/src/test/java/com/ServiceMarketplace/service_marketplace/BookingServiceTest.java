package com.ServiceMarketplace.service_marketplace;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ServiceMarketplace.service_marketplace.dto.CreateBookingRequest;
import com.ServiceMarketplace.service_marketplace.exception.InvalidPriceException;
import com.ServiceMarketplace.service_marketplace.exception.ResourceNotFoundException;
import com.ServiceMarketplace.service_marketplace.model.Booking;
import com.ServiceMarketplace.service_marketplace.model.BookingStatus;
import com.ServiceMarketplace.service_marketplace.model.Service;
import com.ServiceMarketplace.service_marketplace.repository.BookingRepository;
import com.ServiceMarketplace.service_marketplace.repository.ServiceRepository;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private BookingService bookingService;

    private Service mockService;

    @BeforeEach
    void setUp() {
        mockService = new Service();
        mockService.setId("service-123");
        mockService.setUserId("provider-456");
        mockService.setTitle("Math Tutoring");
        mockService.setPriceMin(new BigDecimal("20.00"));
        mockService.setPriceMax(new BigDecimal("80.00"));
        mockService.setPriceUnit("per hour");
    }

    @Test
    void createBooking_validPrice_returnsBooking() {
        CreateBookingRequest request = new CreateBookingRequest(
            "service-123",
            "customer-789",
            new BigDecimal("50.00"),
            Instant.now()
        );

        when(serviceRepository.findById("service-123")).thenReturn(java.util.Optional.of(mockService));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.createBooking(request);

        assertThat(result.getServiceId()).isEqualTo("service-123");
        assertThat(result.getCustomerId()).isEqualTo("customer-789");
        assertThat(result.getProviderId()).isEqualTo("provider-456");
        assertThat(result.getServiceTitle()).isEqualTo("Math Tutoring");
        assertThat(result.getAgreedPrice()).isEqualByComparingTo("50.00");
        assertThat(result.getPriceUnit()).isEqualTo("per hour");
        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void createBooking_priceBelowMin_throwsException() {
        CreateBookingRequest request = new CreateBookingRequest(
            "service-123",
            "customer-789",
            new BigDecimal("10.00"),
            Instant.now()
        );

        when(serviceRepository.findById("service-123")).thenReturn(java.util.Optional.of(mockService));

        assertThatThrownBy(() -> bookingService.createBooking(request))
            .isInstanceOf(InvalidPriceException.class)
            .hasMessageContaining("price");
    }

    @Test
    void createBooking_priceAboveMax_throwsException() {
        CreateBookingRequest request = new CreateBookingRequest(
            "service-123",
            "customer-789",
            new BigDecimal("100.00"),
            Instant.now()
        );

        when(serviceRepository.findById("service-123")).thenReturn(java.util.Optional.of(mockService));

        assertThatThrownBy(() -> bookingService.createBooking(request))
            .isInstanceOf(InvalidPriceException.class)
            .hasMessageContaining("price");
    }

    @Test
    void createBooking_serviceNotFound_throwsException() {
        CreateBookingRequest request = new CreateBookingRequest(
            "nonexistent-service",
            "customer-789",
            new BigDecimal("50.00"),
            Instant.now()
        );

        when(serviceRepository.findById("nonexistent-service")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(request))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}