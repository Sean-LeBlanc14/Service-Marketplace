package com.ServiceMarketplace.service_marketplace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class VerificationRequest {
    
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String code;

    public VerificationRequest(String email, String code){
        this.email = email;
        this.code = code;
    }

    public String getEmail(){
        return this.email;
    }

    public String getCode(){
        return this.code;
    }

}
