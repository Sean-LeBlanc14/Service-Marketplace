package com.ServiceMarketplace.service_marketplace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ResendResponse {
    
    @NotBlank
    private String code;

    private boolean sent;

    @Email
    @NotBlank
    private String email;

    public ResendResponse(String code, String email, boolean sent){
        this.code = code;
        this.sent = sent;
        this.email = email;
    }

    public String getCode(){
        return this.code;
    }

    public String getEmail(){
        return this.email;
    }

}
