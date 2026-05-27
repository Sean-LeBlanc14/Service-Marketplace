package com.ServiceMarketplace.service_marketplace.exception;

public class WebhookProcessingException extends RuntimeException {

    public WebhookProcessingException(String message) {
        super(message);
    }
}