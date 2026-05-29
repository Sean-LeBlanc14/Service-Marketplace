package com.ServiceMarketplace.service_marketplace.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.ServiceMarketplace.service_marketplace.model.BookingStatus;
import lombok.Value;

@Value
public class BookingResponse {

    private String id;
    private String serviceId;
    private String serviceTitle;
    private String customerId;
    private String providerId;
    private String customerName;
    private String providerName;
    private String reviewerName;
    private BigDecimal agreedPrice;
    private String priceUnit;
    private Instant scheduledAt;
    private BookingStatus status;
    private Integer rating;
    private String review;
    private Instant reviewedAt;
    private Instant createdAt;
}
