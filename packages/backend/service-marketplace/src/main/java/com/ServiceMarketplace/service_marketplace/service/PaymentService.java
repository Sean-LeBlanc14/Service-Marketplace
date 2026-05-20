package com.ServiceMarketplace.service_marketplace.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

@Service
public class PaymentService {

    @Value("${stripe.secret-key}")
    private String STRIPE_SECRET_KEY;

    public String createPaymentIntent(BigDecimal amount, String serviceId, String customerId) {
        Stripe.apiKey = STRIPE_SECRET_KEY;

        long amountInCents = amount
            .multiply(new BigDecimal("100"))
            .longValue();

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("usd")
                .putMetadata("serviceId", serviceId)
                .putMetadata("customerId", customerId)
                .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);
            return paymentIntent.getClientSecret();
        } catch (StripeException e) {
            throw new RuntimeException("Failed to create payment intent: " + e.getMessage());
        }
    }
}