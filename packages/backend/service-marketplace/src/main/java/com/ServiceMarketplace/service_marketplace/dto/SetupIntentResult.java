package com.ServiceMarketplace.service_marketplace.dto;

import lombok.Value;

@Value
public class SetupIntentResult {
    private String setupClientSecret;
    private String stripeCustomerId;
    private String setupIntentId;
}
