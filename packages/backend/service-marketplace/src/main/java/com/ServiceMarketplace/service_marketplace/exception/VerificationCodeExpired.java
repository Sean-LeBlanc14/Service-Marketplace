package com.ServiceMarketplace.service_marketplace.exception;

public class VerificationCodeExpired extends RuntimeException{
    
    public VerificationCodeExpired(){
        super("This code has expired!");
    }
}
