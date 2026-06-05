package com.ServiceMarketplace.service_marketplace.dto;

import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class ChangePasswordRequest {

    @Size(min=8)
    private String password;
    
    @Size(min=8)
    private String newPassword;
    
}
