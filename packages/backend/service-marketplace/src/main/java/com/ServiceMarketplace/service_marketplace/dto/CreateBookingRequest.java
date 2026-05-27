package com.ServiceMarketplace.service_marketplace.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class CreateBookingRequest {

    @NotBlank
    private String serviceId;

    @NotNull
    private BigDecimal agreedPrice;

    @NotNull
    private Instant scheduledAt;
}