package com.ServiceMarketplace.service_marketplace.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed; 
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Document(collection = "services")
public class Service {

    @Id
    private String id;

    @NotBlank
    private String title;

    @Indexed
    @NotBlank
    private String category;

    @Indexed
    @NotBlank
    private String userId;

    @NotNull
    private BigDecimal priceMin;

    @NotNull
    private BigDecimal priceMax;

    private String priceUnit;

    @NotBlank
    private String description;

    @NotBlank
    private String location;

    private List<String> tags;

    @CreatedDate
    private Instant createdAt;

}