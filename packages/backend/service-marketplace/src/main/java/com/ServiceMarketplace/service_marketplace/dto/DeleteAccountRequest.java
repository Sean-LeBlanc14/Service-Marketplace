package com.ServiceMarketplace.service_marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class DeleteAccountRequest {
    
    @NotBlank
    private String email;

    @NotBlank
    private String password;
}
