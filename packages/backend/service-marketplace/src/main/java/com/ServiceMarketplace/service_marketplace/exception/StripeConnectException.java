package com.ServiceMarketplace.service_marketplace.exception;

public class StripeConnectException extends RuntimeException {

    public StripeConnectException(String message) {
        super(message);
    }
}
