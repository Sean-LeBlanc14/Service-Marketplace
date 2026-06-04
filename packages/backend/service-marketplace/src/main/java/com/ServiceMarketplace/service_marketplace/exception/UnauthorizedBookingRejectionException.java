package com.ServiceMarketplace.service_marketplace.exception;

public class UnauthorizedBookingRejectionException extends RuntimeException{
    
    public UnauthorizedBookingRejectionException(String message){
        super(message);
    }
}
