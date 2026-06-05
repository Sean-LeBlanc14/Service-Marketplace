package com.ServiceMarketplace.service_marketplace.exception;

public class UnauthorizedChatAccessException extends RuntimeException {
    public UnauthorizedChatAccessException(String message) {
        super(message);
    }
}
