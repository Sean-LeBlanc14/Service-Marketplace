package com.ServiceMarketplace.service_marketplace.exception;

public class EmailSendException extends RuntimeException {
    
    public EmailSendException(String email) {
        super("Failed to send email to: " + email);
    }
}