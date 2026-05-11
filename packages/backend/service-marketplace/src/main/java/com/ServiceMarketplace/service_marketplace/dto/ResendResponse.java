package com.ServiceMarketplace.service_marketplace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResendResponse {
    
    @Size(min=6, max=6)
    @NotBlank
    private String code;

    private boolean sent;

    @Email
    @NotBlank
    private String email;

    public ResendResponse(String code, String email, boolean sent){
        this.code = code;
        this.sent = sent;
    }

    public String getCode(){
        return this.code;
    }

    public String getEmail(){
        return this.email;
    }

}
