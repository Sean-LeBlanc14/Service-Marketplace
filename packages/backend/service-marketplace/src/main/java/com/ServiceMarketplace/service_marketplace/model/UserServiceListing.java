package com.ServiceMarketplace.service_marketplace.model;

import java.util.ArrayList;
import java.util.List;

public class UserServiceListing {

    private String id;

    private String title;

    private String description;

    private String price;

    private boolean isHourly;

    private String location;

    private List<String> tags = new ArrayList<>();

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
