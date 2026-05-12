package com.ServiceMarketplace.service_marketplace.dto;

public class AuthResponse {
    
    private String id;
    private String email;
    private String token;
    
    public AuthResponse(String id, String email, String token) {
        this.id = id;
        this.email = email;
        this.token = token;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getToken(){
        return this.token;
    }
}
