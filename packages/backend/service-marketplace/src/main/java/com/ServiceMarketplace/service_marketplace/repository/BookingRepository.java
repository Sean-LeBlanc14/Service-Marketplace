package com.ServiceMarketplace.service_marketplace.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.ServiceMarketplace.service_marketplace.model.Booking;
import com.ServiceMarketplace.service_marketplace.model.BookingStatus;

@Repository
public interface BookingRepository extends MongoRepository<Booking, String> {
    Optional<Booking> findByStripePaymentIntentId(String stripePaymentIntentId);

    List<Booking> findByProviderId(String providerId);

    List<Booking> findByProviderIdAndStatus(String providerId, BookingStatus status);
}