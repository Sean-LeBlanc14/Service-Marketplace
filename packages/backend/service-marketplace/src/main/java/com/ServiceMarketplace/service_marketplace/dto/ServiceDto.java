package com.ServiceMarketplace.service_marketplace.dto;

import java.math.BigDecimal;
import java.util.List;

public class ServiceDto {

    private String id;
    private String title;
    private String category;
    private String userId;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    private String priceUnit;
    private String description;
    private String location;
    private List<String> tags;

    public ServiceDto(String id, String title, String category, String userId,
                      BigDecimal priceMin, BigDecimal priceMax, String priceUnit,
                      String description, String location, List<String> tags) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.userId = userId;
        this.priceMin = priceMin;
        this.priceMax = priceMax;
        this.priceUnit = priceUnit;
        this.description = description;
        this.location = location;
        this.tags = tags;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getUserId() { return userId; }
    public BigDecimal getPriceMin() { return priceMin; }
    public BigDecimal getPriceMax() { return priceMax; }
    public String getPriceUnit() { return priceUnit; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public List<String> getTags() { return tags; }
}