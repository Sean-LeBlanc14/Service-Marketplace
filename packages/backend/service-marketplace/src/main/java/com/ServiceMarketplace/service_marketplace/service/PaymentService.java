package com.ServiceMarketplace.service_marketplace.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ServiceMarketplace.service_marketplace.exception.InvalidWebhookSignatureException;
import com.ServiceMarketplace.service_marketplace.exception.PaymentProcessingException;
import com.ServiceMarketplace.service_marketplace.exception.ResourceNotFoundException;
import com.ServiceMarketplace.service_marketplace.exception.WebhookProcessingException;
import com.ServiceMarketplace.service_marketplace.model.Booking;
import com.ServiceMarketplace.service_marketplace.model.BookingStatus;
import com.ServiceMarketplace.service_marketplace.repository.BookingRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;

@Service
public class PaymentService {

    @Value("${stripe.secret-key}")
    private String STRIPE_SECRET_KEY;

    @Value("${stripe.webhook-secret}")
    private String STRIPE_WEBHOOK_SECRET;

    private final BookingRepository bookingRepository;

    public PaymentService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

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
            throw new PaymentProcessingException("Failed to create payment intent: " + e.getMessage());
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
    }
}