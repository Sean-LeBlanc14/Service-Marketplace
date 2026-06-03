package com.ServiceMarketplace.service_marketplace.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class SupportResponse {

    @NotBlank
    private Instant reportDate;

    @NotBlank
    private String message;
    
}
