package com.ServiceMarketplace.service_marketplace.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "booking_tokens")
public class BookingToken {

    @Id
    private String id;

    @Indexed(unique = true)
    private String token;

    private String bookingId;

    private BookingTokenAction action;

    private Instant expiresAt;

    private boolean used = false;

    @CreatedDate
    private Instant createdAt;
}
