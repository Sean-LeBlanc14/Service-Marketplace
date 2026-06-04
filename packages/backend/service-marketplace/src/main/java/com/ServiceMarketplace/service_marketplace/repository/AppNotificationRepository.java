package com.ServiceMarketplace.service_marketplace.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ServiceMarketplace.service_marketplace.model.AppNotification;

public interface AppNotificationRepository extends MongoRepository<AppNotification, String> {
    List<AppNotification> findByUserIdOrderByCreatedAtDesc(String userId);
    long countByUserIdAndReadFalse(String userId);
}
