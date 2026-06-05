package com.ServiceMarketplace.service_marketplace.exception;

public class RedundantChangeException extends RuntimeException{
    
    public RedundantChangeException(String message){
        super(message);
    }
}
