package com.ServiceMarketplace.service_marketplace.dto;

import java.time.Instant;

import lombok.Value;

@Value
public class ProviderReviewResponse {

    private String serviceTitle;
    private Integer rating;
    private String review;
    private String reviewerFirstName;
    private Instant reviewedAt;
}
