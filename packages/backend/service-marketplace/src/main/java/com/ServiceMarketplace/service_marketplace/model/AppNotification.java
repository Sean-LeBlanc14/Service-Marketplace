package com.ServiceMarketplace.service_marketplace.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "notifications")
public class AppNotification {
    @Id
    private String id;

    @Indexed
    private String userId;

    private NotificationType type;

    private String title;

    private String body;

    private String referenceId;

    private boolean read;

    @CreatedDate
    private Instant createdAt;
}
