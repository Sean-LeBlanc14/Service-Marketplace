package com.ServiceMarketplace.service_marketplace.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class ConfirmBookingRequest {

    @NotNull
    private BigDecimal confirmedPrice;
}
