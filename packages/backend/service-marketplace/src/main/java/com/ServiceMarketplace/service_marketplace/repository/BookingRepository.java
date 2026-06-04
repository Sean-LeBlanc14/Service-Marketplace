package com.ServiceMarketplace.service_marketplace.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.ServiceMarketplace.service_marketplace.model.Booking;

@Repository
public interface BookingRepository extends MongoRepository<Booking, String> {
    Optional<Booking> findByStripePaymentIntentId(String stripePaymentIntentId);

    List<Booking> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    @Query("{ 'providerId': ?0, 'rating': { $ne: null } }")
    List<Booking> findReviewedBookingsByProviderId(String providerId);
}
