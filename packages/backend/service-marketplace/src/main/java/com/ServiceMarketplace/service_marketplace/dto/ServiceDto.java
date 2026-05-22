package com.ServiceMarketplace.service_marketplace.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Value;

@Value
public class ServiceDto {

    private String id;
    private String title;
    private String category;
    private String userId;
    private String providerName;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    private String priceUnit;
    private String description;
    private String location;
    private List<String> tags;

}
