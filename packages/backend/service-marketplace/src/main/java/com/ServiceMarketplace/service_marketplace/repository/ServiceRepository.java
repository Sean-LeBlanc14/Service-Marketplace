package com.ServiceMarketplace.service_marketplace.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.ServiceMarketplace.service_marketplace.model.Service;

@Repository
public interface ServiceRepository extends MongoRepository<Service, String> {
    
}