package com.ServiceMarketplace.service_marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VerificationRequest {

    @NotBlank
    @Size(min=6, max=6)
    private String code;

    public VerificationRequest(String code){
        this.code = code;
    }

    public String getCode(){
        return this.code;
    }

}
