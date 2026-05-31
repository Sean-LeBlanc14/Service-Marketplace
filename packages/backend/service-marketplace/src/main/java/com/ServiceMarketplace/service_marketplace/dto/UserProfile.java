package com.ServiceMarketplace.service_marketplace.dto;

import java.util.List;

import lombok.Value;

@Value
public class UserProfile {
    
    private String email;

    private String firstName;

    private String lastName;

    private String major;

    private String campus;

    private String bio;

    private boolean isVerified;

    private String role;

    private List<ServiceDto> services;
}
