package com.ServiceMarketplace.service_marketplace.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ServiceMarketplace.service_marketplace.model.Message;

public interface MessageRepository extends MongoRepository<Message, String> {
    List<Message> findByConversationIdOrderByCreatedAtAsc(String conversationId);
    long countByConversationIdAndSenderIdNotAndReadFalse(String conversationId, String senderId);
    List<Message> findByConversationIdAndSenderIdNotAndReadFalse(String conversationId, String senderId);
    long countBySenderIdNotAndConversationIdInAndReadFalse(String senderId, java.util.List<String> conversationIds);
}
