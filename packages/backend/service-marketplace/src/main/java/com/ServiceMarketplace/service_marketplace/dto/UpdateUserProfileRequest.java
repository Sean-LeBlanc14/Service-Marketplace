package com.ServiceMarketplace.service_marketplace.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

public class UpdateUserProfileRequest {

    @Size(max = 800)
    private String bio;

    @Size(max = 25)
    private List<@Valid UserServiceListingRequest> services;

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public List<UserServiceListingRequest> getServices() {
        return services;
    }

    public void setServices(List<UserServiceListingRequest> services) {
        this.services = services;
    }
}
