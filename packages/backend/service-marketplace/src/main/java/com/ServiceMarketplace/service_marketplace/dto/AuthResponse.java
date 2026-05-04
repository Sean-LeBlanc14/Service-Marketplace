package com.ServiceMarketplace.service_marketplace.dto;

public class AuthResponse {
    
    private String id;
    private String email;
    
    public AuthResponse(String id, String email) {
        this.id = id;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }
}
