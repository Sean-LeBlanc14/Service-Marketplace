package com.ServiceMarketplace.service_marketplace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {
    
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min=8)
    private String password;

    public String getEmail(){
        return this.email;
    }

    public String getPassword(){
        return this.password;
    }
}
