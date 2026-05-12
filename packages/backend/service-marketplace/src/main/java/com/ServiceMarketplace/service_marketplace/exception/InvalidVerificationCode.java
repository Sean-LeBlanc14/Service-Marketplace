package com.ServiceMarketplace.service_marketplace.exception;

public class InvalidVerificationCode extends RuntimeException {
    
    public InvalidVerificationCode(){
        super("The code you entered does not match. Please check the code and try again.");
    }
}
