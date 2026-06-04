package com.ServiceMarketplace.service_marketplace.dto;

import java.time.Instant;

import com.ServiceMarketplace.service_marketplace.model.NotificationType;

import lombok.Value;

@Value
public class NotificationResponse {
    String id;
    NotificationType type;
    String title;
    String body;
    String referenceId;
    boolean read;
    Instant createdAt;
}
