package com.ServiceMarketplace.service_marketplace.dto;

import java.util.List;

import lombok.Value;

@Value
public class ProviderProfile {

    private String id;

    private String firstName;

    private String lastName;

    private String major;

    private String campus;

    private String bio;

    private Double averageRating;

    private int reviewCount;

    private List<ServiceDto> services;
}
