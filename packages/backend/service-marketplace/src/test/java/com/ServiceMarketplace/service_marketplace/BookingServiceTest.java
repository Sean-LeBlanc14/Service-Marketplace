package com.ServiceMarketplace.service_marketplace;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

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
import org.springframework.security.core.userdetails.UserDetails;

import com.ServiceMarketplace.service_marketplace.dto.CreateBookingRequest;
import com.ServiceMarketplace.service_marketplace.exception.InvalidPriceException;
import com.ServiceMarketplace.service_marketplace.exception.ResourceNotFoundException;
import com.ServiceMarketplace.service_marketplace.model.Booking;
import com.ServiceMarketplace.service_marketplace.model.BookingStatus;
import com.ServiceMarketplace.service_marketplace.model.Service;
import com.ServiceMarketplace.service_marketplace.model.User;
import com.ServiceMarketplace.service_marketplace.repository.BookingRepository;
import com.ServiceMarketplace.service_marketplace.repository.ServiceRepository;
import com.ServiceMarketplace.service_marketplace.repository.UserRepository;
import com.ServiceMarketplace.service_marketplace.service.BookingService;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private BookingService bookingService;

    private Service mockService;
    private User mockCustomer;

    @BeforeEach
    void setUp() {
        mockService = new Service();
        mockService.setId("service-123");
        mockService.setUserId("provider-456");
        mockService.setTitle("Math Tutoring");
        mockService.setPriceMin(new BigDecimal("20.00"));
        mockService.setPriceMax(new BigDecimal("80.00"));
        mockService.setPriceUnit("per hour");

        mockCustomer = new User();
        mockCustomer.setId("customer-789");
        mockCustomer.setEmail("student@calpoly.edu");

        when(userDetails.getUsername()).thenReturn("student@calpoly.edu");
    }

    @Test
    void createBooking_validPrice_returnsBooking() {
        CreateBookingRequest request = new CreateBookingRequest(
            "service-123",
            new BigDecimal("50.00"),
            Instant.now()
        );

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(serviceRepository.findById("service-123")).thenReturn(Optional.of(mockService));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.createBooking(request, userDetails);

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
            new BigDecimal("10.00"),
            Instant.now()
        );

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(serviceRepository.findById("service-123")).thenReturn(Optional.of(mockService));

        assertThatThrownBy(() -> bookingService.createBooking(request, userDetails))
            .isInstanceOf(InvalidPriceException.class)
            .hasMessageContaining("price");
    }

    @Test
    void createBooking_priceAboveMax_throwsException() {
        CreateBookingRequest request = new CreateBookingRequest(
            "service-123",
            new BigDecimal("100.00"),
            Instant.now()
        );

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(serviceRepository.findById("service-123")).thenReturn(Optional.of(mockService));

        assertThatThrownBy(() -> bookingService.createBooking(request, userDetails))
            .isInstanceOf(InvalidPriceException.class)
            .hasMessageContaining("price");
    }

    @Test
    void createBooking_serviceNotFound_throwsException() {
        CreateBookingRequest request = new CreateBookingRequest(
            "nonexistent-service",
            new BigDecimal("50.00"),
            Instant.now()
        );

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(serviceRepository.findById("nonexistent-service")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(request, userDetails))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}