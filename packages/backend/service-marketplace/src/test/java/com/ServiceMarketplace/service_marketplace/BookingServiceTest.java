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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

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
import com.ServiceMarketplace.service_marketplace.exception.ServiceUnavailableException;
import com.ServiceMarketplace.service_marketplace.exception.UnauthorizedBookingRejectionException;
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
import com.ServiceMarketplace.service_marketplace.service.NotificationService;
import com.ServiceMarketplace.service_marketplace.service.PaymentService;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentService paymentService;
    @Mock private EmailService emailService;
    @Mock private BookingTokenService bookingTokenService;
    @Mock private NotificationService notificationService;
    @Mock private MongoTemplate mongoTemplate;
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
        mockProvider.setLastName("Smith");
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

        verify(emailService).sendBookingRequestedCustomerEmail(
            eq("student@calpoly.edu"), eq("Alice"), eq("Math Tutoring"),
            eq(new BigDecimal("50.00")), eq("per hour"), any(Instant.class),
            Mockito.isNull()
        );
        verify(bookingTokenService).generateTokenPair(any());
        verify(emailService).sendProviderBookingNotificationEmail(
            eq("tutor@calpoly.edu"), eq("Bob"), eq("Alice Student"), eq("Math Tutoring"),
            eq(new BigDecimal("50.00")), eq("per hour"), any(Instant.class),
            eq("http://localhost/confirm/abc"), eq("http://localhost/cancel/xyz")
        );
    }

    @Test
    void createBooking_unavailableService_throwsServiceUnavailableException() {
        CreateBookingRequest request = new CreateBookingRequest("service-123", new BigDecimal("50.00"), Instant.now());
        mockService.setIsAvailable(false);

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(serviceRepository.findById("service-123")).thenReturn(Optional.of(mockService));

        assertThatThrownBy(() -> bookingService.createBooking(request, userDetails))
            .isInstanceOf(ServiceUnavailableException.class);

        verify(paymentService, never()).createSetupIntent(any(), any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_singlePostingService_reservesServiceAtomically() {
        CreateBookingRequest request = new CreateBookingRequest("service-123", new BigDecimal("50.00"), Instant.now());
        mockService.setPostingType("single");
        mockService.setIsAvailable(true);

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(serviceRepository.findById("service-123")).thenReturn(Optional.of(mockService));
        when(userRepository.findById("provider-456")).thenReturn(Optional.of(mockProvider));
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Service.class)))
            .thenReturn(mockService);
        when(paymentService.createSetupIntent(eq("student@calpoly.edu"), eq("Alice Student")))
            .thenReturn(new SetupIntentResult("seti_secret_test", "cus_test_123", "seti_test_id"));
        when(bookingTokenService.generateTokenPair(any()))
            .thenReturn(new TokenPair("http://localhost/confirm/abc", "http://localhost/cancel/xyz"));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingService.createBooking(request, userDetails);

        verify(mongoTemplate).findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Service.class));
        verify(serviceRepository, never()).save(any(Service.class));
    }

    @Test
    void createBooking_singlePostingAlreadyReserved_throwsServiceUnavailableException() {
        CreateBookingRequest request = new CreateBookingRequest("service-123", new BigDecimal("50.00"), Instant.now());
        mockService.setPostingType("single");
        mockService.setIsAvailable(true);

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(serviceRepository.findById("service-123")).thenReturn(Optional.of(mockService));
        when(userRepository.findById("provider-456")).thenReturn(Optional.of(mockProvider));
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Service.class)))
            .thenReturn(null);

        assertThatThrownBy(() -> bookingService.createBooking(request, userDetails))
            .isInstanceOf(ServiceUnavailableException.class);

        verify(paymentService, never()).createSetupIntent(any(), any());
        verify(bookingRepository, never()).save(any());
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
        when(userRepository.findAllById(any())).thenReturn(List.of(mockCustomer, mockProvider));

        var result = bookingService.getCustomerBookings(userDetails);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("booking-123");
        assertThat(result.get(0).getCustomerName()).isEqualTo("Alice Student");
        assertThat(result.get(0).getProviderName()).isEqualTo("Bob Smith");
        assertThat(result.get(0).getReviewerName()).isEqualTo("Alice Student");
        assertThat(result.get(0).getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(userRepository).findAllById(any());
        verify(userRepository, never()).findById(any());
        verify(bookingRepository, never()).findByStatusOrderByCreatedAtDesc(any());
    }

    @Test
    void getCustomerBookings_usesDisplayNameFallbacksInResponse() {
        Booking emailFallbackBooking = createBookingWithStatus(BookingStatus.CONFIRMED);
        emailFallbackBooking.setCustomerId("email-user");
        emailFallbackBooking.setProviderId("id-only-user");

        Booking blankIdBooking = createBookingWithStatus(BookingStatus.CONFIRMED);
        blankIdBooking.setId("blank-id-booking");
        blankIdBooking.setCustomerId(" ");
        blankIdBooking.setProviderId("missing-provider");

        User emailOnlyUser = new User();
        emailOnlyUser.setId("email-user");
        emailOnlyUser.setFirstName(" ");
        emailOnlyUser.setLastName(" ");
        emailOnlyUser.setEmail("fallback@calpoly.edu");

        User idOnlyUser = new User();
        idOnlyUser.setId("id-only-user");
        idOnlyUser.setFirstName(" ");
        idOnlyUser.setLastName(" ");
        idOnlyUser.setEmail(" ");

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(bookingRepository.findByCustomerIdOrderByCreatedAtDesc("customer-789"))
            .thenReturn(List.of(emailFallbackBooking, blankIdBooking));
        when(userRepository.findAllById(any())).thenReturn(List.of(emailOnlyUser, idOnlyUser));

        var result = bookingService.getCustomerBookings(userDetails);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCustomerName()).isEqualTo("fallback@calpoly.edu");
        assertThat(result.get(0).getProviderName()).isEqualTo("id-only-user");
        assertThat(result.get(0).getReviewerName()).isEqualTo("fallback@calpoly.edu");
        assertThat(result.get(1).getCustomerName()).isEmpty();
        assertThat(result.get(1).getProviderName()).isEqualTo("missing-provider");
        assertThat(result.get(1).getReviewerName()).isEmpty();
    }

    @Test
    void getProviderReviews_returnsReviewOnlyResponse() {
        Booking booking = createBookingWithStatus(BookingStatus.COMPLETED);
        booking.setRating(5);
        booking.setReview("Helpful tutoring.");
        booking.setReviewedAt(Instant.now());
        booking.setReviewerName("Alice Student");

        when(bookingRepository.findReviewedBookingsByProviderId("provider-456"))
            .thenReturn(List.of(booking));
        when(userRepository.findAllById(any())).thenReturn(List.of(mockCustomer, mockProvider));

        var result = bookingService.getProviderReviews("provider-456");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getServiceTitle()).isEqualTo("Math Tutoring");
        assertThat(result.get(0).getRating()).isEqualTo(5);
        assertThat(result.get(0).getReview()).isEqualTo("Helpful tutoring.");
        assertThat(result.get(0).getReviewerFirstName()).isEqualTo("Alice");
    }

    @Test
    void getProviderReviews_usesReviewerFirstNameFallbacksInResponse() {
        Booking namedReview = createBookingWithStatus(BookingStatus.COMPLETED);
        namedReview.setRating(5);
        namedReview.setReview("Helpful tutoring.");
        namedReview.setReviewedAt(Instant.now());
        namedReview.setCustomerId("missing-customer");
        namedReview.setReviewerName("Avery Chen");

        Booking namelessReview = createBookingWithStatus(BookingStatus.COMPLETED);
        namelessReview.setId("nameless-review");
        namelessReview.setRating(4);
        namelessReview.setReview("Clear explanations.");
        namelessReview.setReviewedAt(Instant.now());
        namelessReview.setCustomerId(" ");
        namelessReview.setReviewerName(" ");

        when(bookingRepository.findReviewedBookingsByProviderId("provider-456"))
            .thenReturn(List.of(namedReview, namelessReview));
        when(userRepository.findAllById(any())).thenReturn(List.of());

        var result = bookingService.getProviderReviews("provider-456");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getReviewerFirstName()).isEqualTo("Avery");
        assertThat(result.get(1).getReviewerFirstName()).isEmpty();
    }

    @Test
    void submitReview_completedCustomerBooking_savesReview() {
        Booking booking = createBookingWithStatus(BookingStatus.COMPLETED);
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
    void submitReview_uncompletedBooking_throwsException() {
        Booking booking = createBookingWithStatus(BookingStatus.PENDING_PAYMENT);
        SubmitReviewRequest request = new SubmitReviewRequest();
        request.setRating(4);
        request.setReview("Helpful.");

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(bookingRepository.findById("booking-123")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.submitReview("booking-123", request, userDetails))
            .isInstanceOf(InvalidBookingReviewException.class)
            .hasMessageContaining("completed");
    }

    @Test
    void submitReview_existingReview_throwsException() {
        Booking booking = createBookingWithStatus(BookingStatus.COMPLETED);
        booking.setRating(5);
        booking.setReview("Already reviewed.");
        SubmitReviewRequest request = new SubmitReviewRequest();
        request.setRating(4);
        request.setReview("Trying again.");

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(bookingRepository.findById("booking-123")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.submitReview("booking-123", request, userDetails))
            .isInstanceOf(InvalidBookingReviewException.class)
            .hasMessageContaining("already been reviewed");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void submitReview_confirmedBooking_throwsException() {
        Booking booking = createBookingWithStatus(BookingStatus.CONFIRMED);
        SubmitReviewRequest request = new SubmitReviewRequest();
        request.setRating(4);
        request.setReview("Helpful.");

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(bookingRepository.findById("booking-123")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.submitReview("booking-123", request, userDetails))
            .isInstanceOf(InvalidBookingReviewException.class)
            .hasMessageContaining("completed");
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
        when(userRepository.findById("customer-789")).thenReturn(Optional.of(mockCustomer));
        when(paymentService.createAndConfirmPaymentIntent(
            eq(new BigDecimal("60.00")), eq("cus_test_123"),
            eq("service-123"), eq("customer-789"), eq("acct_test_provider")))
            .thenReturn(new PaymentIntentResult("pi_secret_test", "pi_test_id"));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse result = bookingService.confirmBooking(
            "booking-001", new ConfirmBookingRequest(new BigDecimal("60.00")), userDetails);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(result.getAgreedPrice()).isEqualByComparingTo("60.00");
        verify(notificationService).send(
            eq("customer-789"),
            any(),
            eq("Booking Confirmed"),
            eq("Bob confirmed your booking for Math Tutoring"),
            eq("booking-001")
        );
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
    void cancelBooking_byCustomer_cancelsAndNotifiesProvider() {
        Booking pending = buildAwaitingBooking();

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(bookingRepository.findById("booking-001")).thenReturn(Optional.of(pending));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById("provider-456")).thenReturn(Optional.of(mockProvider));
        when(userRepository.findById("customer-789")).thenReturn(Optional.of(mockCustomer));

        BookingResponse result = bookingService.cancelBooking("booking-001", userDetails);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(paymentService).cleanupStripeCustomer("cus_test_123");
        verify(emailService).sendBookingCancelledProviderEmail(
            eq("tutor@calpoly.edu"), eq("Bob"), eq("Alice Student"),
            eq("Math Tutoring"), any(Instant.class), any()
        );
    }

    @Test
    void cancelBooking_byProvider_cancelsAndNotifiesCustomer() {
        Booking pending = buildAwaitingBooking();

        when(userDetails.getUsername()).thenReturn("tutor@calpoly.edu");
        when(userRepository.findByEmail("tutor@calpoly.edu")).thenReturn(Optional.of(mockProvider));
        when(bookingRepository.findById("booking-001")).thenReturn(Optional.of(pending));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById("customer-789")).thenReturn(Optional.of(mockCustomer));
        when(userRepository.findById("provider-456")).thenReturn(Optional.of(mockProvider));

        BookingResponse result = bookingService.cancelBooking("booking-001", userDetails);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(paymentService).cleanupStripeCustomer("cus_test_123");
        verify(emailService, never()).sendBookingCancelledProviderEmail(any(), any(), any(), any(), any(), any());
        verify(emailService).sendBookingCancelledCustomerEmail(
            eq("student@calpoly.edu"), eq("Alice"), eq("Bob Smith"),
            eq("Math Tutoring"), any(Instant.class), any()
        );
    }

    @Test
    void cancelBooking_confirmedBooking_refundsPaymentAndCleansUpCustomer() {
        Booking confirmed = buildAwaitingBooking();
        confirmed.setStatus(BookingStatus.CONFIRMED);
        confirmed.setStripePaymentIntentId("pi_paid_123");

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(bookingRepository.findById("booking-001")).thenReturn(Optional.of(confirmed));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById("provider-456")).thenReturn(Optional.of(mockProvider));
        when(userRepository.findById("customer-789")).thenReturn(Optional.of(mockCustomer));

        BookingResponse result = bookingService.cancelBooking("booking-001", userDetails);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        InOrder inOrder = Mockito.inOrder(bookingRepository, paymentService);
        inOrder.verify(bookingRepository).save(confirmed);
        inOrder.verify(paymentService).refundPaymentIntent("pi_paid_123", true);
        inOrder.verify(paymentService).cleanupStripeCustomer("cus_test_123");
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
    void cancelBooking_alreadyCancelled_throwsBookingStateException() {
        Booking booking = buildAwaitingBooking();
        booking.setStatus(BookingStatus.CANCELLED);

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(bookingRepository.findById("booking-001")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking("booking-001", userDetails))
            .isInstanceOf(BookingStateException.class);
    }

    @Test
    void cancelBooking_alreadyRejected_throwsBookingStateException() {
        Booking booking = buildAwaitingBooking();
        booking.setStatus(BookingStatus.REJECTED);

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(bookingRepository.findById("booking-001")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking("booking-001", userDetails))
            .isInstanceOf(BookingStateException.class)
            .hasMessageContaining("rejected");
    }

    @Test
    void cancelBooking_completedBooking_throwsBookingStateException() {
        Booking booking = buildAwaitingBooking();
        booking.setStatus(BookingStatus.COMPLETED);

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(bookingRepository.findById("booking-001")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking("booking-001", userDetails))
            .isInstanceOf(BookingStateException.class)
            .hasMessageContaining("Completed");
    }

    @Test
    void rejectBooking_byProvider_rejectsAndNotifiesCustomer() {
        Booking pending = buildAwaitingBooking();

        when(userDetails.getUsername()).thenReturn("tutor@calpoly.edu");
        when(userRepository.findByEmail("tutor@calpoly.edu")).thenReturn(Optional.of(mockProvider));
        when(bookingRepository.findById("booking-001")).thenReturn(Optional.of(pending));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById("customer-789")).thenReturn(Optional.of(mockCustomer));

        BookingResponse result = bookingService.rejectBooking("booking-001", userDetails);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.REJECTED);
        verify(paymentService, never()).refundPaymentIntent(any(), anyBoolean());
        verify(paymentService).cleanupStripeCustomer("cus_test_123");
        verify(emailService).sendBookingRejectedCustomerEmail(
            eq("student@calpoly.edu"), eq("Alice"), eq("Bob Smith"),
            eq("Math Tutoring"), any(Instant.class), any()
        );
    }

    @Test
    void rejectBooking_byCustomer_rejectsAndNotifiesProvider() {
        Booking pending = buildAwaitingBooking();

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(bookingRepository.findById("booking-001")).thenReturn(Optional.of(pending));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById("provider-456")).thenReturn(Optional.of(mockProvider));

        BookingResponse result = bookingService.rejectBooking("booking-001", userDetails);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.REJECTED);
        verify(paymentService, never()).refundPaymentIntent(any(), anyBoolean());
        verify(paymentService).cleanupStripeCustomer("cus_test_123");
        verify(emailService).sendBookingRejectedProviderEmail(
            eq("tutor@calpoly.edu"), eq("Bob"), eq("Alice Student"),
            eq("Math Tutoring"), any(Instant.class), any()
        );
    }

    @Test
    void rejectBooking_confirmedBooking_throwsBookingStateException() {
        Booking confirmed = buildAwaitingBooking();
        confirmed.setStatus(BookingStatus.CONFIRMED);
        confirmed.setStripePaymentIntentId("pi_paid_123");

        when(userDetails.getUsername()).thenReturn("tutor@calpoly.edu");
        when(userRepository.findByEmail("tutor@calpoly.edu")).thenReturn(Optional.of(mockProvider));
        when(bookingRepository.findById("booking-001")).thenReturn(Optional.of(confirmed));

        assertThatThrownBy(() -> bookingService.rejectBooking("booking-001", userDetails))
            .isInstanceOf(BookingStateException.class)
            .hasMessageContaining("awaiting provider confirmation");
        verify(bookingRepository, never()).save(any());
        verify(paymentService, never()).refundPaymentIntent(any(), anyBoolean());
    }

    @Test
    void rejectBooking_unauthorizedUser_throwsUnauthorizedBookingRejectionException() {
        Booking pending = buildAwaitingBooking();
        User stranger = buildStranger();

        when(userDetails.getUsername()).thenReturn("stranger@calpoly.edu");
        when(userRepository.findByEmail("stranger@calpoly.edu")).thenReturn(Optional.of(stranger));
        when(bookingRepository.findById("booking-001")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> bookingService.rejectBooking("booking-001", userDetails))
            .isInstanceOf(UnauthorizedBookingRejectionException.class);
    }

    @Test
    void rejectBooking_completedBooking_throwsBookingStateException() {
        Booking booking = buildAwaitingBooking();
        booking.setStatus(BookingStatus.COMPLETED);

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(bookingRepository.findById("booking-001")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.rejectBooking("booking-001", userDetails))
            .isInstanceOf(BookingStateException.class)
            .hasMessageContaining("Completed");
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
    void processTokenAction_cancelToken_cancelsAndNotifiesCustomer() {
        Booking pending = buildAwaitingBooking();

        when(bookingTokenService.validateAndConsume("valid-cancel-token"))
            .thenReturn(new TokenResult("booking-001", BookingTokenAction.CANCEL));
        when(bookingRepository.findById("booking-001")).thenReturn(Optional.of(pending));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById("customer-789")).thenReturn(Optional.of(mockCustomer));
        when(userRepository.findById("provider-456")).thenReturn(Optional.of(mockProvider));

        BookingTokenAction result = bookingService.processTokenAction("valid-cancel-token");

        assertThat(result).isEqualTo(BookingTokenAction.CANCEL);
        verify(paymentService).cleanupStripeCustomer("cus_test_123");
        verify(emailService, never()).sendBookingCancelledProviderEmail(any(), any(), any(), any(), any(), any());
        verify(emailService).sendBookingCancelledCustomerEmail(
            eq("student@calpoly.edu"), eq("Alice"), eq("Bob Smith"),
            eq("Math Tutoring"), any(Instant.class), any()
        );
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

    @Test
    void createBooking_singlePostingSetupFailure_releasesReservation() {
        CreateBookingRequest request = new CreateBookingRequest("service-123", new BigDecimal("50.00"), Instant.now());
        mockService.setPostingType("single");
        mockService.setIsAvailable(true);

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(serviceRepository.findById("service-123")).thenReturn(Optional.of(mockService));
        when(userRepository.findById("provider-456")).thenReturn(Optional.of(mockProvider));
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Service.class)))
            .thenReturn(mockService);
        when(paymentService.createSetupIntent(eq("student@calpoly.edu"), eq("Alice Student")))
            .thenThrow(new RuntimeException("Stripe setup failed"));

        assertThatThrownBy(() -> bookingService.createBooking(request, userDetails))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Stripe setup failed");

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Service.class));
    }

    @Test
    void getProviderBookingRequests_returnsAwaitingRequests() {
        Booking booking = createBookingWithStatus(BookingStatus.AWAITING_PROVIDER_CONFIRMATION);

        when(userDetails.getUsername()).thenReturn("tutor@calpoly.edu");
        when(userRepository.findByEmail("tutor@calpoly.edu")).thenReturn(Optional.of(mockProvider));
        when(bookingRepository.findByProviderIdAndStatusOrderByCreatedAtDesc(
            "provider-456",
            BookingStatus.AWAITING_PROVIDER_CONFIRMATION
        )).thenReturn(List.of(booking));
        when(userRepository.findAllById(any())).thenReturn(List.of(mockCustomer, mockProvider));

        List<BookingResponse> result = bookingService.getProviderBookingRequests(userDetails);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(BookingStatus.AWAITING_PROVIDER_CONFIRMATION);
    }

    @Test
    void getProviderScheduledBookings_returnsConfirmedBookings() {
        Booking booking = createBookingWithStatus(BookingStatus.CONFIRMED);

        when(userDetails.getUsername()).thenReturn("tutor@calpoly.edu");
        when(userRepository.findByEmail("tutor@calpoly.edu")).thenReturn(Optional.of(mockProvider));
        when(bookingRepository.findByProviderIdAndStatusOrderByCreatedAtDesc("provider-456", BookingStatus.CONFIRMED))
            .thenReturn(List.of(booking));
        when(userRepository.findAllById(any())).thenReturn(List.of(mockCustomer, mockProvider));

        List<BookingResponse> result = bookingService.getProviderScheduledBookings(userDetails);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void getProviderCompletedBookings_returnsCompletedBookings() {
        Booking booking = createBookingWithStatus(BookingStatus.COMPLETED);

        when(userDetails.getUsername()).thenReturn("tutor@calpoly.edu");
        when(userRepository.findByEmail("tutor@calpoly.edu")).thenReturn(Optional.of(mockProvider));
        when(bookingRepository.findByProviderIdAndStatusOrderByCreatedAtDesc("provider-456", BookingStatus.COMPLETED))
            .thenReturn(List.of(booking));
        when(userRepository.findAllById(any())).thenReturn(List.of(mockCustomer, mockProvider));

        List<BookingResponse> result = bookingService.getProviderCompletedBookings(userDetails);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(BookingStatus.COMPLETED);
    }

    @Test
    void getCustomerScheduledBookings_returnsConfirmedCustomerBookings() {
        Booking booking = createBookingWithStatus(BookingStatus.CONFIRMED);

        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(bookingRepository.findByCustomerIdAndStatusOrderByCreatedAtDesc("customer-789", BookingStatus.CONFIRMED))
            .thenReturn(List.of(booking));
        when(userRepository.findAllById(any())).thenReturn(List.of(mockCustomer, mockProvider));

        List<BookingResponse> result = bookingService.getCustomerScheduledBookings(userDetails);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void getCustomerBookings_emptyBookings_doesNotLookupUsers() {
        when(userRepository.findByEmail("student@calpoly.edu")).thenReturn(Optional.of(mockCustomer));
        when(bookingRepository.findByCustomerIdOrderByCreatedAtDesc("customer-789")).thenReturn(List.of());

        List<BookingResponse> result = bookingService.getCustomerBookings(userDetails);

        assertThat(result).isEmpty();
        verify(userRepository, never()).findAllById(any());
    }

    @Test
    void getProviderBookingRequests_missingUser_throwsUsernameNotFoundException() {
        when(userDetails.getUsername()).thenReturn("missing@calpoly.edu");
        when(userRepository.findByEmail("missing@calpoly.edu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getProviderBookingRequests(userDetails))
            .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void rejectBooking_providerRejectsRequestAndNotifiesCustomer() {
        Booking booking = createBookingWithStatus(BookingStatus.AWAITING_PROVIDER_CONFIRMATION);

        when(userDetails.getUsername()).thenReturn("tutor@calpoly.edu");
        when(userRepository.findByEmail("tutor@calpoly.edu")).thenReturn(Optional.of(mockProvider));
        when(bookingRepository.findById("booking-123")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById("customer-789")).thenReturn(Optional.of(mockCustomer));

        bookingService.rejectBooking("booking-123", userDetails);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.REJECTED);
        verify(bookingRepository).save(booking);
        verify(emailService).sendBookingRejectedCustomerEmail(
            eq("student@calpoly.edu"),
            eq("Alice"),
            eq("Bob Smith"),
            eq("Math Tutoring"),
            any(Instant.class),
            eq("booking-123")
        );
    }

    @Test
    void rejectBooking_unauthorizedUser_throwsUnauthorizedRejection() {
        Booking booking = createBookingWithStatus(BookingStatus.CONFIRMED);
        User stranger = buildStranger();

        when(userDetails.getUsername()).thenReturn("stranger@calpoly.edu");
        when(userRepository.findByEmail("stranger@calpoly.edu")).thenReturn(Optional.of(stranger));
        when(bookingRepository.findById("booking-123")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.rejectBooking("booking-123", userDetails))
            .isInstanceOf(com.ServiceMarketplace.service_marketplace.exception.UnauthorizedBookingRejectionException.class);
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
