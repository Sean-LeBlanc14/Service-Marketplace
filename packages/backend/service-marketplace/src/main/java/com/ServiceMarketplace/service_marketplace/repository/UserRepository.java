package com.ServiceMarketplace.service_marketplace.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.ServiceMarketplace.service_marketplace.model.User;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
