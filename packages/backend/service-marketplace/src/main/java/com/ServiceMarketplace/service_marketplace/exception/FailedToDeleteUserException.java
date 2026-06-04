package com.ServiceMarketplace.service_marketplace.exception;

public class FailedToDeleteUserException extends RuntimeException{
    
    public FailedToDeleteUserException(String message){
        super(message);
    }
}
