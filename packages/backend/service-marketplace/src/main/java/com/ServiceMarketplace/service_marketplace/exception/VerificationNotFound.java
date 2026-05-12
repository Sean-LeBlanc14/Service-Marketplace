package com.ServiceMarketplace.service_marketplace.exception;

public class VerificationNotFound extends RuntimeException{
    
    public VerificationNotFound(){
        super("Verification Credentials for this User are not found!");
    }
}
