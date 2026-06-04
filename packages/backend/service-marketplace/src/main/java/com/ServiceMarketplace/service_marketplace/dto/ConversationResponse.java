package com.ServiceMarketplace.service_marketplace.dto;

import java.time.Instant;

import lombok.Value;

@Value
public class ConversationResponse {
    String id;
    String serviceId;
    String serviceTitle;
    String customerId;
    String customerName;
    String providerId;
    String providerName;
    String lastMessagePreview;
    Instant lastMessageAt;
    long unreadCount;
    Instant createdAt;
}
