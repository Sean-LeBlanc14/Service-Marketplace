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
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import com.ServiceMarketplace.service_marketplace.dto.ConnectOnboardingResponse;
import com.ServiceMarketplace.service_marketplace.dto.ConnectStatusResponse;
import com.ServiceMarketplace.service_marketplace.dto.PaymentIntentResult;
import com.ServiceMarketplace.service_marketplace.dto.SetupIntentResult;
import com.ServiceMarketplace.service_marketplace.exception.InvalidWebhookSignatureException;
import com.ServiceMarketplace.service_marketplace.exception.PaymentProcessingException;
import com.ServiceMarketplace.service_marketplace.exception.StripeConnectException;
import com.ServiceMarketplace.service_marketplace.exception.WebhookProcessingException;
import com.ServiceMarketplace.service_marketplace.model.Booking;
import com.ServiceMarketplace.service_marketplace.model.BookingStatus;
import com.ServiceMarketplace.service_marketplace.model.User;
import com.ServiceMarketplace.service_marketplace.repository.BookingRepository;
import com.ServiceMarketplace.service_marketplace.repository.UserRepository;
import com.ServiceMarketplace.service_marketplace.service.EmailService;
import com.ServiceMarketplace.service_marketplace.service.PaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.model.SetupIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodListParams;
import com.stripe.param.SetupIntentCreateParams;

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
        ReflectionTestUtils.setField(paymentService, "STRIPE_SECRET_KEY", "sk_test");
        ReflectionTestUtils.setField(paymentService, "STRIPE_WEBHOOK_SECRET", "whsec_test");
        ReflectionTestUtils.setField(paymentService, "connectReturnUrl", "https://app.test/return");
        ReflectionTestUtils.setField(paymentService, "connectRefreshUrl", "https://app.test/refresh");
        ReflectionTestUtils.setField(paymentService, "platformFeePercent", 10);

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
    void createSetupIntent_success_returnsClientSecretAndCustomerIds() {
        try (MockedStatic<Customer> customerMock = Mockito.mockStatic(Customer.class);
             MockedStatic<SetupIntent> setupIntentMock = Mockito.mockStatic(SetupIntent.class)) {
            Customer customer = mock(Customer.class);
            when(customer.getId()).thenReturn("cus_test_123");
            SetupIntent setupIntent = mock(SetupIntent.class);
            when(setupIntent.getClientSecret()).thenReturn("seti_secret_test");
            when(setupIntent.getId()).thenReturn("seti_test_123");

            customerMock.when(() -> Customer.create(any(CustomerCreateParams.class))).thenReturn(customer);
            setupIntentMock.when(() -> SetupIntent.create(any(SetupIntentCreateParams.class))).thenReturn(setupIntent);

            SetupIntentResult result = paymentService.createSetupIntent("student@calpoly.edu", "Alice Student");

            assertThat(result.getSetupClientSecret()).isEqualTo("seti_secret_test");
            assertThat(result.getStripeCustomerId()).isEqualTo("cus_test_123");
            assertThat(result.getSetupIntentId()).isEqualTo("seti_test_123");
        }
    }

    @Test
    void createSetupIntent_stripeFailure_throwsPaymentProcessingException() throws Exception {
        try (MockedStatic<Customer> customerMock = Mockito.mockStatic(Customer.class)) {
            customerMock.when(() -> Customer.create(any(CustomerCreateParams.class)))
                .thenThrow(mock(StripeException.class));

            assertThatThrownBy(() -> paymentService.createSetupIntent("student@calpoly.edu", "Alice Student"))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessageContaining("Failed to create setup intent");
        }
    }

    @Test
    void createAndConfirmPaymentIntent_successWithProviderDestination_returnsPaymentIntentResult() {
        try (MockedStatic<PaymentMethod> paymentMethodMock = Mockito.mockStatic(PaymentMethod.class);
             MockedStatic<PaymentIntent> paymentIntentMock = Mockito.mockStatic(PaymentIntent.class)) {
            PaymentMethod savedMethod = mock(PaymentMethod.class);
            when(savedMethod.getId()).thenReturn("pm_test_123");
            PaymentMethodCollection methodCollection = mock(PaymentMethodCollection.class);
            when(methodCollection.getData()).thenReturn(List.of(savedMethod));
            PaymentIntent paymentIntent = mock(PaymentIntent.class);
            when(paymentIntent.getClientSecret()).thenReturn("pi_secret_test");
            when(paymentIntent.getId()).thenReturn("pi_test_123");

            paymentMethodMock.when(() -> PaymentMethod.list(any(PaymentMethodListParams.class)))
                .thenReturn(methodCollection);
            paymentIntentMock.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                .thenReturn(paymentIntent);

            PaymentIntentResult result = paymentService.createAndConfirmPaymentIntent(
                new BigDecimal("50.00"),
                "cus_test_123",
                "service-123",
                "customer-789",
                "acct_test_provider"
            );

            assertThat(result.getClientSecret()).isEqualTo("pi_secret_test");
            assertThat(result.getPaymentIntentId()).isEqualTo("pi_test_123");
        }
    }

    @Test
    void createAndConfirmPaymentIntent_noSavedPaymentMethod_throwsPaymentProcessingException() {
        try (MockedStatic<PaymentMethod> paymentMethodMock = Mockito.mockStatic(PaymentMethod.class)) {
            PaymentMethodCollection methodCollection = mock(PaymentMethodCollection.class);
            when(methodCollection.getData()).thenReturn(List.of());
            paymentMethodMock.when(() -> PaymentMethod.list(any(PaymentMethodListParams.class)))
                .thenReturn(methodCollection);

            assertThatThrownBy(() -> paymentService.createAndConfirmPaymentIntent(
                new BigDecimal("50.00"),
                "cus_test_123",
                "service-123",
                "customer-789",
                null
            )).isInstanceOf(PaymentProcessingException.class)
                .hasMessage("No payment method saved for this booking");
        }
    }

    @Test
    void createAndConfirmPaymentIntent_stripeFailure_throwsPaymentProcessingException() throws Exception {
        try (MockedStatic<PaymentMethod> paymentMethodMock = Mockito.mockStatic(PaymentMethod.class)) {
            paymentMethodMock.when(() -> PaymentMethod.list(any(PaymentMethodListParams.class)))
                .thenThrow(mock(StripeException.class));

            assertThatThrownBy(() -> paymentService.createAndConfirmPaymentIntent(
                new BigDecimal("50.00"),
                "cus_test_123",
                "service-123",
                "customer-789",
                null
            )).isInstanceOf(PaymentProcessingException.class)
                .hasMessageContaining("Failed to process payment");
        }
    }

    @Test
    void cleanupStripeCustomer_stripeFailureIsIgnored() throws Exception {
        try (MockedStatic<Customer> customerMock = Mockito.mockStatic(Customer.class)) {
            Customer customer = mock(Customer.class);
            customerMock.when(() -> Customer.retrieve("cus_test_123")).thenReturn(customer);
            when(customer.delete()).thenThrow(mock(StripeException.class));

            paymentService.cleanupStripeCustomer("cus_test_123");

            customerMock.verify(() -> Customer.retrieve("cus_test_123"));
        }
    }

    @Test
    void initiateOnboarding_newUserStripeAccount_createsAccountAndAccountLink() {
        try (MockedStatic<Account> accountMock = Mockito.mockStatic(Account.class);
             MockedStatic<AccountLink> accountLinkMock = Mockito.mockStatic(AccountLink.class)) {
            Account account = mock(Account.class);
            when(account.getId()).thenReturn("acct_test_provider");
            AccountLink accountLink = mock(AccountLink.class);
            when(accountLink.getUrl()).thenReturn("https://stripe.test/onboard");

            accountMock.when(() -> Account.create(any(AccountCreateParams.class))).thenReturn(account);
            accountLinkMock.when(() -> AccountLink.create(any(AccountLinkCreateParams.class))).thenReturn(accountLink);
            when(userRepository.findByEmail("tutor@calpoly.edu")).thenReturn(Optional.of(mockProvider));

            org.springframework.security.core.userdetails.UserDetails userDetails = mock(org.springframework.security.core.userdetails.UserDetails.class);
            when(userDetails.getUsername()).thenReturn("tutor@calpoly.edu");

            ConnectOnboardingResponse result = paymentService.initiateOnboarding(userDetails);

            assertThat(result.getAccountId()).isEqualTo("acct_test_provider");
            assertThat(result.getOnboardingUrl()).isEqualTo("https://stripe.test/onboard");
            verify(userRepository).save(mockProvider);
        }
    }

    @Test
    void initiateOnboarding_missingUser_throwsUsernameNotFoundException() {
        org.springframework.security.core.userdetails.UserDetails userDetails = mock(org.springframework.security.core.userdetails.UserDetails.class);
        when(userDetails.getUsername()).thenReturn("missing@calpoly.edu");
        when(userRepository.findByEmail("missing@calpoly.edu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.initiateOnboarding(userDetails))
            .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void initiateOnboarding_stripeFailure_throwsStripeConnectException() throws Exception {
        try (MockedStatic<Account> accountMock = Mockito.mockStatic(Account.class)) {
            mockProvider.setStripeAccountId(null);
            accountMock.when(() -> Account.create(any(AccountCreateParams.class)))
                .thenThrow(mock(StripeException.class));
            when(userRepository.findByEmail("tutor@calpoly.edu")).thenReturn(Optional.of(mockProvider));

            org.springframework.security.core.userdetails.UserDetails userDetails = mock(org.springframework.security.core.userdetails.UserDetails.class);
            when(userDetails.getUsername()).thenReturn("tutor@calpoly.edu");

            assertThatThrownBy(() -> paymentService.initiateOnboarding(userDetails))
                .isInstanceOf(StripeConnectException.class)
                .hasMessageContaining("Failed to initiate Stripe Connect onboarding");
        }
    }

    @Test
    void getConnectStatus_withoutAccount_returnsDisconnectedStatus() {
        mockProvider.setStripeAccountId(null);
        when(userRepository.findByEmail("tutor@calpoly.edu")).thenReturn(Optional.of(mockProvider));
        org.springframework.security.core.userdetails.UserDetails userDetails = mock(org.springframework.security.core.userdetails.UserDetails.class);
        when(userDetails.getUsername()).thenReturn("tutor@calpoly.edu");

        ConnectStatusResponse result = paymentService.getConnectStatus(userDetails);

        assertThat(result.getAccountId()).isNull();
        assertThat(result.isChargesEnabled()).isFalse();
        assertThat(result.isDetailsSubmitted()).isFalse();
        assertThat(result.isPayoutsEnabled()).isFalse();
    }

    @Test
    void getConnectStatus_withAccount_returnsStripeStatus() {
        try (MockedStatic<Account> accountMock = Mockito.mockStatic(Account.class)) {
            mockProvider.setStripeAccountId("acct_test_provider");
            Account account = mock(Account.class);
            when(account.getChargesEnabled()).thenReturn(true);
            when(account.getDetailsSubmitted()).thenReturn(true);
            when(account.getPayoutsEnabled()).thenReturn(false);
            accountMock.when(() -> Account.retrieve("acct_test_provider")).thenReturn(account);
            when(userRepository.findByEmail("tutor@calpoly.edu")).thenReturn(Optional.of(mockProvider));
            org.springframework.security.core.userdetails.UserDetails userDetails = mock(org.springframework.security.core.userdetails.UserDetails.class);
            when(userDetails.getUsername()).thenReturn("tutor@calpoly.edu");

            ConnectStatusResponse result = paymentService.getConnectStatus(userDetails);

            assertThat(result.getAccountId()).isEqualTo("acct_test_provider");
            assertThat(result.isChargesEnabled()).isTrue();
            assertThat(result.isDetailsSubmitted()).isTrue();
            assertThat(result.isPayoutsEnabled()).isFalse();
        }
    }

    @Test
    void getConnectStatus_stripeFailure_throwsStripeConnectException() throws Exception {
        try (MockedStatic<Account> accountMock = Mockito.mockStatic(Account.class)) {
            mockProvider.setStripeAccountId("acct_test_provider");
            accountMock.when(() -> Account.retrieve("acct_test_provider"))
                .thenThrow(mock(StripeException.class));
            when(userRepository.findByEmail("tutor@calpoly.edu")).thenReturn(Optional.of(mockProvider));
            org.springframework.security.core.userdetails.UserDetails userDetails = mock(org.springframework.security.core.userdetails.UserDetails.class);
            when(userDetails.getUsername()).thenReturn("tutor@calpoly.edu");

            assertThatThrownBy(() -> paymentService.getConnectStatus(userDetails))
                .isInstanceOf(StripeConnectException.class)
                .hasMessageContaining("Failed to retrieve Stripe Connect status");
        }
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

    @Test
    void handleWebhook_deserializationFailure_throwsWebhookProcessingException() throws Exception {
        Event event = mock(Event.class);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);

        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.deserializeUnsafe()).thenThrow(new RuntimeException("bad payload"));

        try (MockedStatic<Webhook> webhookMock = Mockito.mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(any(), any(), any()))
                .thenReturn(event);

            assertThatThrownBy(() -> paymentService.handleWebhook("payload", "sig-header"))
                .isInstanceOf(WebhookProcessingException.class);
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
