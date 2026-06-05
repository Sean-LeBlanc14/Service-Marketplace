package com.ServiceMarketplace.service_marketplace.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ServiceMarketplace.service_marketplace.model.Conversation;

public interface ConversationRepository extends MongoRepository<Conversation, String> {
    List<Conversation> findByCustomerIdOrProviderIdOrderByLastMessageAtDesc(String customerId, String providerId);
    Optional<Conversation> findByServiceIdAndCustomerIdAndProviderId(String serviceId, String customerId, String providerId);
}
