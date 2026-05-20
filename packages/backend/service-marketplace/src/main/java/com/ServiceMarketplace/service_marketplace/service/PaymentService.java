package com.ServiceMarketplace.service_marketplace.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    public String createPaymentIntent(BigDecimal amount, String serviceId, String customerId) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}