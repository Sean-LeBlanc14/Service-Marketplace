package com.ServiceMarketplace.service_marketplace.exception;

public class InvalidEmailDomainException extends RuntimeException {

    public InvalidEmailDomainException(String message) {
        super(message);
    }
}
