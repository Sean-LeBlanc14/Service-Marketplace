package com.ServiceMarketplace.service_marketplace.exception;

public class FailedToDeleteUser extends RuntimeException{
    
    public FailedToDeleteUser(String message){
        super(message);
    }
}
