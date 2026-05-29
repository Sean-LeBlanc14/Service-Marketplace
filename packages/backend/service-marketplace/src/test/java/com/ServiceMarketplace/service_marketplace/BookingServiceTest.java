package com.ServiceMarketplace.service_marketplace;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;

import com.ServiceMarketplace.service_marketplace.dto.BookingResponse;
import com.ServiceMarketplace.service_marketplace.dto.ConfirmBookingRequest;
import com.ServiceMarketplace.service_marketplace.dto.CreateBookingRequest;
import com.ServiceMarketplace.service_marketplace.dto.CreateBookingResponse;
import com.ServiceMarketplace.service_marketplace.dto.PaymentIntentResult;
import com.ServiceMarketplace.service_marketplace.dto.SetupIntentResult;
import com.ServiceMarketplace.service_marketplace.dto.SubmitReviewRequest;
import com.ServiceMarketplace.service_marketplace.exception.BookingStateException;
import com.ServiceMarketplace.service_marketplace.exception.BookingTokenException;
import com.ServiceMarketplace.service_marketplace.exception.InvalidBookingReviewException;
import com.ServiceMarketplace.service_marketplace.exception.InvalidPriceException;
import com.ServiceMarketplace.service_marketplace.exception.ResourceNotFoundException;
import com.ServiceMarketplace.service_marketplace.model.Booking;
import com.ServiceMarketplace.service_marketplace.model.BookingStatus;
import com.ServiceMarketplace.service_marketplace.model.BookingTokenAction;
import com.ServiceMarketplace.service_marketplace.model.Service;
import com.ServiceMarketplace.service_marketplace.model.User;
import com.ServiceMarketplace.service_marketplace.repository.BookingRepository;
import com.ServiceMarketplace.service_marketplace.repository.ServiceRepository;
import com.ServiceMarketplace.service_marketplace.repository.UserRepository;
import com.ServiceMarketplace.service_marketplace.service.BookingService;
import com.ServiceMarketplace.service_marketplace.service.BookingTokenService;
import com.ServiceMarketplace.service_marketplace.service.BookingTokenService.TokenPair;
import com.ServiceMarketplace.service_marketplace.service.BookingTokenService.TokenResult;
import com.ServiceMarketplace.service_marketplace.service.EmailService;
import com.ServiceMarketplace.service_marketplace.service.PaymentService;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentService paymentService;
    @Mock private EmailService emailService;
    @Mock private BookingTokenService bookingTokenService;
    @Mock private UserDetails userDetails;

    @InjectMocks
    private BookingService bookingService;

    private Service mockService;
    private User mockCustomer;
    private User mockProvider;

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
        mockCustomer.setFirstName("Alice");
        mockCustomer.setLastName("Student");

        mockProvider = new User();
        mockProvider.setId("provider-456");
        mockProvider.setEmail("tutor@calpoly.edu");
        mockProvider.setFirstName("Bob");
        mockProvider.setStripeAccountId("acct_test_provider");

        lenient().when(userDetails.getUsername()).thenReturn("student@calpoly.edu");
    }

    @Test
    void createBooking_validPrice_createsSetupIntentAndNotifiesProvider() {
        CreateBookingRequest request = new CreateBookingRequest("service-123", new BigDecimal("50.00"), Instant.now());

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(serviceRepository.findById("service-123")).thenReturn(Optional.of(mockService));
        when(userRepository.findById("provider-456")).thenReturn(Optional.of(mockProvider));
        when(paymentService.createSetupIntent(eq("student@calpoly.edu"), eq("Alice Student")))
            .thenReturn(new SetupIntentResult("seti_secret_test", "cus_test_123", "seti_test_id"));
        when(bookingTokenService.generateTokenPair(any()))
            .thenReturn(new TokenPair("http://localhost/confirm/abc", "http://localhost/cancel/xyz"));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateBookingResponse result = bookingService.createBooking(request, userDetails);

        assertThat(result.getBooking().getServiceId()).isEqualTo("service-123");
        assertThat(result.getBooking().getCustomerId()).isEqualTo("customer-789");
        assertThat(result.getBooking().getProviderId()).isEqualTo("provider-456");
        assertThat(result.getBooking().getServiceTitle()).isEqualTo("Math Tutoring");
        assertThat(result.getBooking().getAgreedPrice()).isEqualByComparingTo("50.00");
        assertThat(result.getBooking().getStatus()).isEqualTo(BookingStatus.AWAITING_PROVIDER_CONFIRMATION);
        assertThat(result.getSetupClientSecret()).isEqualTo("seti_secret_test");

        verify(bookingTokenService).generateTokenPair(any());
        verify(emailService).sendProviderBookingNotificationEmail(
            eq("tutor@calpoly.edu"), eq("Bob"), eq("Alice Student"), eq("Math Tutoring"),
            eq(new BigDecimal("50.00")), eq("per hour"), any(Instant.class),
            eq("http://localhost/confirm/abc"), eq("http://localhost/cancel/xyz")
        );
    }

    @Test
    void createBooking_priceBelowMin_throwsInvalidPriceException() {
        CreateBookingRequest request = new CreateBookingRequest("service-123", new BigDecimal("10.00"), Instant.now());

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(serviceRepository.findById("service-123")).thenReturn(Optional.of(mockService));

        assertThatThrownBy(() -> bookingService.createBooking(request, userDetails))
            .isInstanceOf(InvalidPriceException.class)
            .hasMessageContaining("price");
    }

    @Test
    void createBooking_priceAboveMax_throwsInvalidPriceException() {
        CreateBookingRequest request = new CreateBookingRequest("service-123", new BigDecimal("100.00"), Instant.now());

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(serviceRepository.findById("service-123")).thenReturn(Optional.of(mockService));

        assertThatThrownBy(() -> bookingService.createBooking(request, userDetails))
            .isInstanceOf(InvalidPriceException.class)
            .hasMessageContaining("price");
    }

    @Test
    void createBooking_serviceNotFound_throwsResourceNotFoundException() {
        CreateBookingRequest request = new CreateBookingRequest("nonexistent", new BigDecimal("50.00"), Instant.now());

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(serviceRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(request, userDetails))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCustomerBookings_returnsCurrentCustomerBookings() {
        Booking booking = createBookingWithStatus(BookingStatus.CONFIRMED);

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(bookingRepository.findByCustomerIdOrderByCreatedAtDesc("customer-789"))
            .thenReturn(List.of(booking));

        var result = bookingService.getCustomerBookings(userDetails);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("booking-123");
        assertThat(result.get(0).getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void submitReview_confirmedCustomerBooking_savesReview() {
        Booking booking = createBookingWithStatus(BookingStatus.CONFIRMED);
        SubmitReviewRequest request = new SubmitReviewRequest();
        request.setRating(5);
        request.setReview(" Great help with the final project. ");

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(bookingRepository.findById("booking-123")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = bookingService.submitReview("booking-123", request, userDetails);

        assertThat(result.getRating()).isEqualTo(5);
        assertThat(result.getReview()).isEqualTo("Great help with the final project.");
        assertThat(result.getReviewerName()).isEqualTo("Alice Student");
        assertThat(result.getReviewedAt()).isNotNull();
        verify(bookingRepository).save(booking);
    }

    @Test
    void submitReview_pendingBooking_throwsException() {
        Booking booking = createBookingWithStatus(BookingStatus.PENDING_PAYMENT);
        SubmitReviewRequest request = new SubmitReviewRequest();
        request.setRating(4);
        request.setReview("Helpful.");

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(bookingRepository.findById("booking-123")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.submitReview("booking-123", request, userDetails))
            .isInstanceOf(InvalidBookingReviewException.class)
            .hasMessageContaining("confirmed");
    }

    @Test
    void submitReview_otherCustomerBooking_throwsException() {
        Booking booking = createBookingWithStatus(BookingStatus.CONFIRMED);
        booking.setCustomerId("another-customer");
        SubmitReviewRequest request = new SubmitReviewRequest();
        request.setRating(4);
        request.setReview("Helpful.");

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(bookingRepository.findById("booking-123")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.submitReview("booking-123", request, userDetails))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void confirmBooking_validPrice_chargesCustomerAndSetsPendingPayment() {
        Booking pending = buildAwaitingBooking();

        when(userDetails.getUsername()).thenReturn("tutor@calpoly.edu");
        when(userRepository.findByEmail("tutor@calpoly.edu")).thenReturn(Optional.of(mockProvider));
        when(bookingRepository.findById("booking-001")).thenReturn(Optional.of(pending));
        when(serviceRepository.findById("service-123")).thenReturn(Optional.of(mockService));
        when(paymentService.createAndConfirmPaymentIntent(
            eq(new BigDecimal("60.00")), eq("cus_test_123"),
            eq("service-123"), eq("customer-789"), eq("acct_test_provider")))
            .thenReturn(new PaymentIntentResult("pi_secret_test", "pi_test_id"));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse result = bookingService.confirmBooking(
            "booking-001", new ConfirmBookingRequest(new BigDecimal("60.00")), userDetails);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(result.getAgreedPrice()).isEqualByComparingTo("60.00");
    }

    @Test
    void confirmBooking_priceOutOfRange_throwsInvalidPriceException() {
        Booking pending = buildAwaitingBooking();

        when(userDetails.getUsername()).thenReturn("tutor@calpoly.edu");
        when(userRepository.findByEmail("tutor@calpoly.edu")).thenReturn(Optional.of(mockProvider));
        when(bookingRepository.findById("booking-001")).thenReturn(Optional.of(pending));
        when(serviceRepository.findById("service-123")).thenReturn(Optional.of(mockService));

        assertThatThrownBy(() -> bookingService.confirmBooking(
            "booking-001", new ConfirmBookingRequest(new BigDecimal("150.00")), userDetails))
            .isInstanceOf(InvalidPriceException.class);
    }

    @Test
    void confirmBooking_wrongProvider_throwsAccessDeniedException() {
        Booking pending = buildAwaitingBooking();
        User stranger = buildStranger();

        when(userDetails.getUsername()).thenReturn("stranger@calpoly.edu");
        when(userRepository.findByEmail("stranger@calpoly.edu")).thenReturn(Optional.of(stranger));
        when(bookingRepository.findById("booking-001")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> bookingService.confirmBooking(
            "booking-001", new ConfirmBookingRequest(new BigDecimal("50.00")), userDetails))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void confirmBooking_alreadyConfirmed_throwsBookingStateException() {
        Booking booking = buildAwaitingBooking();
        booking.setStatus(BookingStatus.CONFIRMED);

        when(userDetails.getUsername()).thenReturn("tutor@calpoly.edu");
        when(userRepository.findByEmail("tutor@calpoly.edu")).thenReturn(Optional.of(mockProvider));
        when(bookingRepository.findById("booking-001")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.confirmBooking(
            "booking-001", new ConfirmBookingRequest(new BigDecimal("50.00")), userDetails))
            .isInstanceOf(BookingStateException.class);
    }

    @Test
    void cancelBooking_byCustomer_cancelsAndCleansUpStripeCustomer() {
        Booking pending = buildAwaitingBooking();

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(bookingRepository.findById("booking-001")).thenReturn(Optional.of(pending));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse result = bookingService.cancelBooking("booking-001", userDetails);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(paymentService).cleanupStripeCustomer("cus_test_123");
    }

    @Test
    void cancelBooking_byProvider_cancelsAndCleansUpStripeCustomer() {
        Booking pending = buildAwaitingBooking();

        when(userDetails.getUsername()).thenReturn("tutor@calpoly.edu");
        when(userRepository.findByEmail("tutor@calpoly.edu")).thenReturn(Optional.of(mockProvider));
        when(bookingRepository.findById("booking-001")).thenReturn(Optional.of(pending));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse result = bookingService.cancelBooking("booking-001", userDetails);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(paymentService).cleanupStripeCustomer("cus_test_123");
    }

    @Test
    void cancelBooking_unauthorizedUser_throwsAccessDeniedException() {
        Booking pending = buildAwaitingBooking();
        User stranger = buildStranger();

        when(userDetails.getUsername()).thenReturn("stranger@calpoly.edu");
        when(userRepository.findByEmail("stranger@calpoly.edu")).thenReturn(Optional.of(stranger));
        when(bookingRepository.findById("booking-001")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> bookingService.cancelBooking("booking-001", userDetails))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cancelBooking_notAwaitingConfirmation_throwsBookingStateException() {
        Booking booking = buildAwaitingBooking();
        booking.setStatus(BookingStatus.CONFIRMED);

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(bookingRepository.findById("booking-001")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking("booking-001", userDetails))
            .isInstanceOf(BookingStateException.class);
    }

    @Test
    void processTokenAction_confirmToken_confirmsBookingAtExistingPrice() {
        Booking pending = buildAwaitingBooking();

        when(bookingTokenService.validateAndConsume("valid-confirm-token"))
            .thenReturn(new TokenResult("booking-001", BookingTokenAction.CONFIRM));
        when(bookingRepository.findById("booking-001")).thenReturn(Optional.of(pending));
        when(userRepository.findById("provider-456")).thenReturn(Optional.of(mockProvider));
        when(serviceRepository.findById("service-123")).thenReturn(Optional.of(mockService));
        when(paymentService.createAndConfirmPaymentIntent(
            eq(new BigDecimal("50.00")), eq("cus_test_123"),
            eq("service-123"), eq("customer-789"), eq("acct_test_provider")))
            .thenReturn(new PaymentIntentResult("pi_secret", "pi_id"));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingTokenAction result = bookingService.processTokenAction("valid-confirm-token");

        assertThat(result).isEqualTo(BookingTokenAction.CONFIRM);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void processTokenAction_cancelToken_cancelsAndCleansUp() {
        Booking pending = buildAwaitingBooking();

        when(bookingTokenService.validateAndConsume("valid-cancel-token"))
            .thenReturn(new TokenResult("booking-001", BookingTokenAction.CANCEL));
        when(bookingRepository.findById("booking-001")).thenReturn(Optional.of(pending));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingTokenAction result = bookingService.processTokenAction("valid-cancel-token");

        assertThat(result).isEqualTo(BookingTokenAction.CANCEL);
        verify(paymentService).cleanupStripeCustomer("cus_test_123");
    }

    @Test
    void processTokenAction_invalidToken_throwsBookingTokenException() {
        when(bookingTokenService.validateAndConsume("bad-token"))
            .thenThrow(new BookingTokenException("Invalid or unrecognized confirmation link"));

        assertThatThrownBy(() -> bookingService.processTokenAction("bad-token"))
            .isInstanceOf(BookingTokenException.class);
    }

    @Test
    void processTokenAction_expiredToken_throwsBookingTokenException() {
        when(bookingTokenService.validateAndConsume("expired-token"))
            .thenThrow(new BookingTokenException("This confirmation link has expired"));

        assertThatThrownBy(() -> bookingService.processTokenAction("expired-token"))
            .isInstanceOf(BookingTokenException.class)
            .hasMessageContaining("expired");
    }

    private Booking createBookingWithStatus(BookingStatus status) {
        Booking booking = new Booking();
        booking.setId("booking-123");
        booking.setServiceId("service-123");
        booking.setCustomerId("customer-789");
        booking.setProviderId("provider-456");
        booking.setServiceTitle("Math Tutoring");
        booking.setAgreedPrice(new BigDecimal("50.00"));
        booking.setPriceUnit("per hour");
        booking.setScheduledAt(Instant.now());
        booking.setStatus(status);
        booking.setCreatedAt(Instant.now());
        booking.setStripeCustomerId("cus_test_123");
        booking.setStripeSetupIntentId("seti_test_id");
        return booking;
    }

    private Booking buildAwaitingBooking() {
        Booking booking = createBookingWithStatus(BookingStatus.AWAITING_PROVIDER_CONFIRMATION);
        booking.setId("booking-001");
        return booking;
    }

    private User buildStranger() {
        User stranger = new User();
        stranger.setId("stranger-999");
        stranger.setEmail("stranger@calpoly.edu");
        return stranger;
    }
}
