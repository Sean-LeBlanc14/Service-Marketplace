package com.ServiceMarketplace.service_marketplace.dto;

import jakarta.validation.constraints.Size;

public class UpdateUserProfileRequest {

    @Size(max = 800)
    private String bio;

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
