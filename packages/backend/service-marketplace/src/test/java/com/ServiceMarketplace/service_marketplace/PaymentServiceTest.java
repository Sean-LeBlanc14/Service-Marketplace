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
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ServiceMarketplace.service_marketplace.exception.InvalidWebhookSignatureException;
import com.ServiceMarketplace.service_marketplace.model.Booking;
import com.ServiceMarketplace.service_marketplace.model.BookingStatus;
import com.ServiceMarketplace.service_marketplace.model.User;
import com.ServiceMarketplace.service_marketplace.repository.BookingRepository;
import com.ServiceMarketplace.service_marketplace.repository.UserRepository;
import com.ServiceMarketplace.service_marketplace.service.EmailService;
import com.ServiceMarketplace.service_marketplace.service.PaymentService;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private PaymentService paymentService;

    private Booking mockBooking;
    private User mockCustomer;
    private User mockProvider;

    @BeforeEach
    void setUp() {
        mockBooking = new Booking();
        mockBooking.setId("booking-001");
        mockBooking.setStripePaymentIntentId("pi_test_123");
        mockBooking.setStripeCustomerId("cus_test_123");
        mockBooking.setCustomerId("customer-789");
        mockBooking.setProviderId("provider-456");
        mockBooking.setServiceTitle("Math Tutoring");
        mockBooking.setAgreedPrice(new BigDecimal("50.00"));
        mockBooking.setPriceUnit("per hour");
        mockBooking.setScheduledAt(Instant.now());
        mockBooking.setStatus(BookingStatus.PENDING_PAYMENT);

        mockCustomer = new User();
        mockCustomer.setId("customer-789");
        mockCustomer.setEmail("student@calpoly.edu");
        mockCustomer.setFirstName("Alice");

        mockProvider = new User();
        mockProvider.setId("provider-456");
        mockProvider.setEmail("tutor@calpoly.edu");
        mockProvider.setFirstName("Bob");
    }

    @Test
    void handleWebhook_paymentSucceeded_updatesBookingToConfirmedAndSendsEmails() throws Exception {
        Event mockEvent = buildMockEvent("payment_intent.succeeded", "pi_test_123");
        try (MockedStatic<Webhook> webhookMock = Mockito.mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(any(), any(), any()))
                .thenReturn(mockEvent);

            when(bookingRepository.findByStripePaymentIntentId("pi_test_123")).thenReturn(Optional.of(mockBooking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById("customer-789")).thenReturn(Optional.of(mockCustomer));
            when(userRepository.findById("provider-456")).thenReturn(Optional.of(mockProvider));

            paymentService.handleWebhook("payload", "sig-header");

            assertThat(mockBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            verify(bookingRepository).save(mockBooking);
            verify(emailService).sendBookingConfirmedCustomerEmail(
                eq("student@calpoly.edu"), eq("Alice"), eq("Math Tutoring"),
                any(BigDecimal.class), eq("per hour"), any(Instant.class), eq("booking-001")
            );
            verify(emailService).sendBookingConfirmedProviderEmail(
                eq("tutor@calpoly.edu"), eq("Bob"), eq("Math Tutoring"),
                any(BigDecimal.class), eq("per hour"), any(Instant.class), eq("booking-001")
            );
        }
    }

    @Test
    void handleWebhook_paymentFailed_updatesBookingToCancelledAndCleansUpStripeCustomer() throws Exception {
        Event mockEvent = buildMockEvent("payment_intent.payment_failed", "pi_test_123");
        try (MockedStatic<Webhook> webhookMock = Mockito.mockStatic(Webhook.class);
             MockedStatic<Customer> customerMock = Mockito.mockStatic(Customer.class)) {

            webhookMock.when(() -> Webhook.constructEvent(any(), any(), any()))
                .thenReturn(mockEvent);

            Customer mockStripeCustomer = mock(Customer.class);
            customerMock.when(() -> Customer.retrieve("cus_test_123")).thenReturn(mockStripeCustomer);

            when(bookingRepository.findByStripePaymentIntentId("pi_test_123")).thenReturn(Optional.of(mockBooking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            paymentService.handleWebhook("payload", "sig-header");

            assertThat(mockBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            verify(bookingRepository).save(mockBooking);
            customerMock.verify(() -> Customer.retrieve("cus_test_123"));
            verify(emailService, never()).sendBookingConfirmedCustomerEmail(any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Test
    void handleWebhook_invalidSignature_throwsInvalidWebhookSignatureException() {
        try (MockedStatic<Webhook> webhookMock = Mockito.mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(any(), any(), any()))
                .thenThrow(new RuntimeException("Invalid signature"));

            assertThatThrownBy(() -> paymentService.handleWebhook("payload", "bad-sig"))
                .isInstanceOf(InvalidWebhookSignatureException.class);
        }
    }

    @Test
    void handleWebhook_unknownEventType_noBookingUpdate() throws Exception {
        Event mockEvent = buildMockEvent("customer.created", null);
        try (MockedStatic<Webhook> webhookMock = Mockito.mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(any(), any(), any()))
                .thenReturn(mockEvent);

            paymentService.handleWebhook("payload", "sig-header");

            verify(bookingRepository, never()).findByStripePaymentIntentId(any());
            verify(bookingRepository, never()).save(any());
        }
    }

    private Event buildMockEvent(String eventType, String paymentIntentId) throws Exception {
        Event event = mock(Event.class);
        when(event.getType()).thenReturn(eventType);

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);

        if (paymentIntentId != null) {
            PaymentIntent paymentIntent = mock(PaymentIntent.class);
            when(paymentIntent.getId()).thenReturn(paymentIntentId);
            when(deserializer.deserializeUnsafe()).thenReturn(paymentIntent);
        } else {
            when(deserializer.deserializeUnsafe()).thenReturn(mock(StripeObject.class));
        }

        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        return event;
    }
}
