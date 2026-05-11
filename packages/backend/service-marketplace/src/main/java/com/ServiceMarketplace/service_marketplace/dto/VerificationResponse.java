package com.ServiceMarketplace.service_marketplace.dto;

public class VerificationResponse {
    
    private String email;

    private String code;

    public VerificationResponse(String email, String Code){
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
