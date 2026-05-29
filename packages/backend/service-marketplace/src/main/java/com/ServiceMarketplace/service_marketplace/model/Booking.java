package com.ServiceMarketplace.service_marketplace.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "bookings")
public class Booking {
    @Id
    private String id;

    @Indexed
    private String serviceId;

    @Indexed
    private String customerId;

    @Indexed
    private String providerId;

    private String serviceTitle;

    private BigDecimal agreedPrice;

    private String priceUnit;

    private Instant scheduledAt;

    private BookingStatus status;

    private String stripePaymentIntentId;

    private Integer rating;

    private String review;

    private Instant reviewedAt;

    @CreatedDate
    private Instant createdAt;
}
