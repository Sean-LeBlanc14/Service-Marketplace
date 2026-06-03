package com.ServiceMarketplace.service_marketplace.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class SupportResponse {

    @NotNull
    private Instant reportDate;

    @NotBlank
    private String message;
    
}
