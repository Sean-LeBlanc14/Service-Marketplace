package com.ServiceMarketplace.service_marketplace.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed; 
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Document(collection = "support")
public class SupportEntry {
    
    @Id
    @Indexed
    private String id;

    @NotNull
    private String reporterEmail;

    @NotBlank
    @Size(min = 10, max = 1000)
    private String context;

    @NotNull
    private SupportEntryType supportEntryType;

    @CreatedDate
    private Instant createdAt;
}
