package com.ServiceMarketplace.service_marketplace.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ServiceMarketplace.service_marketplace.model.BookingToken;

public interface BookingTokenRepository extends MongoRepository<BookingToken, String> {

    Optional<BookingToken> findByToken(String token);
}
