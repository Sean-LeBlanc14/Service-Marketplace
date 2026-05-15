package com.ServiceMarketplace.service_marketplace.dto;

import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class VerificationRequest {

    @Size(min=6, max=6)
    private String code;

}
