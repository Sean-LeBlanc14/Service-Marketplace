package com.ServiceMarketplace.service_marketplace.dto;

import java.math.BigDecimal;

import com.ServiceMarketplace.service_marketplace.model.MessageType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequest {
    @NotNull
    private MessageType type;
    private String content;
    private BigDecimal offeredPrice;
}
