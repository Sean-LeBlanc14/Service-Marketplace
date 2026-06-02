package com.ServiceMarketplace.service_marketplace.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ServiceMarketplace.service_marketplace.dto.ConnectOnboardingResponse;
import com.ServiceMarketplace.service_marketplace.dto.ConnectStatusResponse;
import com.ServiceMarketplace.service_marketplace.dto.PaymentIntentResult;
import com.ServiceMarketplace.service_marketplace.dto.SetupIntentResult;
import com.ServiceMarketplace.service_marketplace.exception.InvalidWebhookSignatureException;
import com.ServiceMarketplace.service_marketplace.exception.PaymentProcessingException;
import com.ServiceMarketplace.service_marketplace.exception.ResourceNotFoundException;
import com.ServiceMarketplace.service_marketplace.exception.StripeConnectException;
import com.ServiceMarketplace.service_marketplace.exception.WebhookProcessingException;
import com.ServiceMarketplace.service_marketplace.model.Booking;
import com.ServiceMarketplace.service_marketplace.model.BookingStatus;
import com.ServiceMarketplace.service_marketplace.model.User;
import com.ServiceMarketplace.service_marketplace.repository.BookingRepository;
import com.ServiceMarketplace.service_marketplace.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.Customer;
import com.stripe.model.Event;
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

@Service
public class PaymentService {

    @Value("${stripe.secret-key}")
    private String STRIPE_SECRET_KEY;

    @Value("${stripe.webhook-secret}")
    private String STRIPE_WEBHOOK_SECRET;

    @Value("${stripe.connect.return-url}")
    private String connectReturnUrl;

    @Value("${stripe.connect.refresh-url}")
    private String connectRefreshUrl;

    @Value("${stripe.platform-fee-percent:10}")
    private int platformFeePercent;

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public PaymentService(BookingRepository bookingRepository, UserRepository userRepository, EmailService emailService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public SetupIntentResult createSetupIntent(String customerEmail, String customerName) {
        Stripe.apiKey = STRIPE_SECRET_KEY;

        try {
            CustomerCreateParams customerParams = CustomerCreateParams.builder()
                .setEmail(customerEmail)
                .setName(customerName)
                .build();
            Customer customer = Customer.create(customerParams);

            SetupIntentCreateParams setupParams = SetupIntentCreateParams.builder()
                .setCustomer(customer.getId())
                .addPaymentMethodType("card")
                .setUsage(SetupIntentCreateParams.Usage.OFF_SESSION)
                .build();
            SetupIntent setupIntent = SetupIntent.create(setupParams);

            return new SetupIntentResult(setupIntent.getClientSecret(), customer.getId(), setupIntent.getId());
        } catch (StripeException e) {
            throw new PaymentProcessingException("Failed to create setup intent: " + e.getMessage());
        }
    }

    public PaymentIntentResult createAndConfirmPaymentIntent(BigDecimal amount, String stripeCustomerId, String serviceId, String customerId, String providerStripeAccountId) {
        Stripe.apiKey = STRIPE_SECRET_KEY;

        long amountInCents = amount
            .multiply(new BigDecimal("100"))
            .longValue();

        try {
            PaymentMethodListParams listParams = PaymentMethodListParams.builder()
                .setCustomer(stripeCustomerId)
                .setType(PaymentMethodListParams.Type.CARD)
                .build();
            PaymentMethodCollection methods = PaymentMethod.list(listParams);

            List<PaymentMethod> paymentMethods = methods.getData();
            if (paymentMethods.isEmpty()) {
                throw new PaymentProcessingException("No payment method saved for this booking");
            }

            String paymentMethodId = paymentMethods.get(0).getId();

            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("usd")
                .setCustomer(stripeCustomerId)
                .setPaymentMethod(paymentMethodId)
                .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.AUTOMATIC)
                .setConfirm(true)
                .setOffSession(true)
                .putMetadata("serviceId", serviceId)
                .putMetadata("customerId", customerId);

            if (providerStripeAccountId != null) {
                long feeInCents = Math.round(amountInCents * platformFeePercent / 100.0);
                paramsBuilder
                    .setApplicationFeeAmount(feeInCents)
                    .setTransferData(PaymentIntentCreateParams.TransferData.builder()
                        .setDestination(providerStripeAccountId)
                        .build());
            }

            PaymentIntent paymentIntent = PaymentIntent.create(paramsBuilder.build());
            return new PaymentIntentResult(paymentIntent.getClientSecret(), paymentIntent.getId());
        } catch (StripeException e) {
            throw new PaymentProcessingException("Failed to process payment: " + e.getMessage());
        }
    }

    public void cleanupStripeCustomer(String stripeCustomerId) {
        if (stripeCustomerId == null) return;
        Stripe.apiKey = STRIPE_SECRET_KEY;
        try {
            Customer customer = Customer.retrieve(stripeCustomerId);
            customer.delete();
        } catch (StripeException ignored) {
            // cleanup failure should not block the cancellation flow
        }
    }

    public ConnectOnboardingResponse initiateOnboarding(UserDetails userDetails) {
        Stripe.apiKey = STRIPE_SECRET_KEY;

        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        try {
            String accountId = user.getStripeAccountId();

            if (accountId == null) {
                AccountCreateParams accountParams = AccountCreateParams.builder()
                    .setType(AccountCreateParams.Type.EXPRESS)
                    .setEmail(user.getEmail())
                    .build();
                Account account = Account.create(accountParams);
                accountId = account.getId();
                user.setStripeAccountId(accountId);
                userRepository.save(user);
            }

            AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder()
                .setAccount(accountId)
                .setRefreshUrl(connectRefreshUrl)
                .setReturnUrl(connectReturnUrl)
                .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                .build();

            AccountLink accountLink = AccountLink.create(linkParams);
            return new ConnectOnboardingResponse(accountId, accountLink.getUrl());
        } catch (StripeException e) {
            throw new StripeConnectException("Failed to initiate Stripe Connect onboarding: " + e.getMessage());
        }
    }

    public ConnectStatusResponse getConnectStatus(UserDetails userDetails) {
        Stripe.apiKey = STRIPE_SECRET_KEY;

        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String accountId = user.getStripeAccountId();

        if (accountId == null) {
            return new ConnectStatusResponse(null, false, false, false);
        }

        try {
            Account account = Account.retrieve(accountId);
            return new ConnectStatusResponse(
                accountId,
                account.getChargesEnabled(),
                account.getDetailsSubmitted(),
                account.getPayoutsEnabled()
            );
        } catch (StripeException e) {
            throw new StripeConnectException("Failed to retrieve Stripe Connect status: " + e.getMessage());
        }
    }

    public void handleWebhook(String payload, String sigHeader) {
        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, STRIPE_WEBHOOK_SECRET);
        } catch (Exception e) {
            throw new InvalidWebhookSignatureException("Invalid webhook signature: " + e.getMessage());
        }

        StripeObject stripeObject = event.getDataObjectDeserializer()
            .getObject()
            .orElseThrow(() -> new WebhookProcessingException("Failed to deserialize webhook event"));

        switch (event.getType()) {
            case "payment_intent.succeeded" -> {
                PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
                updateBookingStatus(paymentIntent.getId(), BookingStatus.CONFIRMED);
            }
            case "payment_intent.payment_failed" -> {
                PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
                updateBookingStatus(paymentIntent.getId(), BookingStatus.CANCELLED);
            }
            default -> { }
        }
    }

    private void updateBookingStatus(String paymentIntentId, BookingStatus status) {
        Booking booking = bookingRepository.findByStripePaymentIntentId(paymentIntentId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", paymentIntentId));
        booking.setStatus(status);
        bookingRepository.save(booking);

        if (status == BookingStatus.CONFIRMED) {
            sendConfirmationEmails(booking);
        } else if (status == BookingStatus.CANCELLED) {
            cleanupStripeCustomer(booking.getStripeCustomerId());
        }
    }

    private void sendConfirmationEmails(Booking booking) {
        userRepository.findById(booking.getCustomerId()).ifPresent(customer ->
            emailService.sendBookingConfirmedCustomerEmail(
                customer.getEmail(),
                customer.getFirstName(),
                booking.getServiceTitle(),
                booking.getAgreedPrice(),
                booking.getPriceUnit(),
                booking.getScheduledAt(),
                booking.getId()
            )
        );

        userRepository.findById(booking.getProviderId()).ifPresent(provider ->
            emailService.sendBookingConfirmedProviderEmail(
                provider.getEmail(),
                provider.getFirstName(),
                booking.getServiceTitle(),
                booking.getAgreedPrice(),
                booking.getPriceUnit(),
                booking.getScheduledAt(),
                booking.getId()
            )
        );
    }
}
