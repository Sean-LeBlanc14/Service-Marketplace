package com.ServiceMarketplace.service_marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VerificationRequest {
    
    //If we use JWT instead of email, with the current implementation it would work, 
    //But if we wait to save user until the user verifies email, that will raise issues
    //As having a JWT implies that there is a saved user to authenticate

    @NotBlank
    @Size(min=6, max=6)
    private String code;

    public VerificationRequest(String email, String code){
        this.code = code;
    }

    public String getCode(){
        return this.code;
    }

}
