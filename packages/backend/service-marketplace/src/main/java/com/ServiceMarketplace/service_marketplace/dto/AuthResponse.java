package com.ServiceMarketplace.service_marketplace.dto;

import lombok.Value;

@Value
public class AuthResponse {
    
    private String id;
    private String email;
    private String token;

}
