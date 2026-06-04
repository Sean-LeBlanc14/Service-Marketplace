package com.ServiceMarketplace.service_marketplace.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "conversations")
public class Conversation {
    @Id
    private String id;

    @Indexed
    private String serviceId;

    private String serviceTitle;

    @Indexed
    private String customerId;

    @Indexed
    private String providerId;

    private String lastMessagePreview;

    private Instant lastMessageAt;

    @CreatedDate
    private Instant createdAt;
}
