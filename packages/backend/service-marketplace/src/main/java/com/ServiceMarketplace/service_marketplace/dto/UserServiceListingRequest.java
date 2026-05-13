package com.ServiceMarketplace.service_marketplace.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserServiceListingRequest {

    @Size(max = 80)
    private String id;

    @NotBlank
    @Size(max = 120)
    private String title;

    @NotBlank
    @Size(max = 1000)
    private String description;

    @NotBlank
    @Size(max = 40)
    private String price;

    private boolean isHourly;

    @NotBlank
    @Size(max = 160)
    private String location;

    @Size(max = 12)
    private List<@Size(max = 40) String> tags = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public boolean getIsHourly() {
        return isHourly;
    }

    public void setIsHourly(boolean isHourly) {
        this.isHourly = isHourly;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : tags;
    }
}
