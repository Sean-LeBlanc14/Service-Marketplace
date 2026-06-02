package com.ServiceMarketplace.service_marketplace.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "reports")
public class Report {

    @Id
    private String id;

    private String reporterId;

    private String listingId;

    private String providerId;

    private String reason;

    private String status = "open";

    @CreatedDate
    private Instant createdAt;

}
