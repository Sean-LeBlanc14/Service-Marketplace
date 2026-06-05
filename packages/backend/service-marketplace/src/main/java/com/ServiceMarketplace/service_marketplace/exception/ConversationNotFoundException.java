package com.ServiceMarketplace.service_marketplace.exception;

public class ConversationNotFoundException extends RuntimeException {
    public ConversationNotFoundException(String id) {
        super("Conversation not found with id: " + id);
    }
}
