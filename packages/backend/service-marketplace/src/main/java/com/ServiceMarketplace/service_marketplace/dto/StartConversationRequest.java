package com.ServiceMarketplace.service_marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StartConversationRequest {
    @NotBlank
    private String serviceId;
}
