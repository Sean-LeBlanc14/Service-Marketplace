package com.ServiceMarketplace.service_marketplace.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.ServiceMarketplace.service_marketplace.model.MessageType;

import lombok.Value;

@Value
public class MessageResponse {
    String id;
    String conversationId;
    String senderId;
    String senderName;
    MessageType type;
    String content;
    BigDecimal offeredPrice;
    boolean offerResponded;
    boolean read;
    Instant createdAt;
}
