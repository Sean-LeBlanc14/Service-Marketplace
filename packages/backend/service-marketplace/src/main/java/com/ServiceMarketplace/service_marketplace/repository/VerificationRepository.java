package com.ServiceMarketplace.service_marketplace.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.ServiceMarketplace.service_marketplace.model.Verification;

@Repository
public interface VerificationRepository extends MongoRepository<Verification, String> {
    
    Optional<Verification> findByEmail(String email);

}
