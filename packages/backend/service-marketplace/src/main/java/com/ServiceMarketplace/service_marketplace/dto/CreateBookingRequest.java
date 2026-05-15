package com.ServiceMarketplace.service_marketplace.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBookingRequest(
    @NotBlank String serviceId,
    @NotBlank String customerId,
    @NotNull BigDecimal agreedPrice,
    @NotNull Instant scheduledAt
) {}