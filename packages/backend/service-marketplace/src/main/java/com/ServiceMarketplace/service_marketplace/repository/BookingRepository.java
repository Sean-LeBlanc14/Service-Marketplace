package com.ServiceMarketplace.service_marketplace.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.ServiceMarketplace.service_marketplace.model.Booking;
import com.ServiceMarketplace.service_marketplace.model.BookingStatus;

@Repository
public interface BookingRepository extends MongoRepository<Booking, String> {
    Optional<Booking> findByStripePaymentIntentId(String stripePaymentIntentId);

    List<Booking> findByProviderIdAndStatusOrderByCreatedAtDesc(String providerId, BookingStatus status);

    List<Booking> findByCustomerIdAndStatusOrderByCreatedAtDesc(String customerId, BookingStatus status);

    List<Booking> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status);

    @Query("{ 'providerId': ?0, 'rating': { $ne: null } }")
    List<Booking> findReviewedBookingsByProviderId(String providerId);

    @Query("{ 'providerId': { $in: ?0 }, 'rating': { $ne: null } }")
    List<Booking> findReviewedBookingsByProviderIdIn(Collection<String> providerIds);
}
