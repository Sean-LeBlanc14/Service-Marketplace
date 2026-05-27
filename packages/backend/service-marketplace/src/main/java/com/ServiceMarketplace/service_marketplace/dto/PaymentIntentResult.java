package com.ServiceMarketplace.service_marketplace.dto;

import lombok.Value;

@Value
public class PaymentIntentResult {
    String clientSecret;
    String paymentIntentId;
}
