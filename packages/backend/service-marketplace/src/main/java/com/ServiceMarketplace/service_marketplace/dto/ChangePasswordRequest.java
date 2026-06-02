package com.ServiceMarketplace.service_marketplace.dto;

import lombok.Value;

@Value
public class ChangePasswordRequest {

    private String password;
    private String newPassword;
    
}
