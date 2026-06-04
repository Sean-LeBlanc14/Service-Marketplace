package com.ServiceMarketplace.service_marketplace.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "messages")
public class Message {
    @Id
    private String id;

    @Indexed
    private String conversationId;

    private String senderId;

    private String senderName;

    private MessageType type;

    private String content;

    private BigDecimal offeredPrice;

    private boolean offerResponded;

    private boolean read;

    @CreatedDate
    private Instant createdAt;
}
