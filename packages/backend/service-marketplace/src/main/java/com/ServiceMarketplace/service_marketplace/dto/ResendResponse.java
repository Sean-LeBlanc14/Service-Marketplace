package com.ServiceMarketplace.service_marketplace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class ResendResponse {
    
    @NotBlank
    private String code;

    @Email
    @NotBlank
    private String email;

}
