package com.ServiceMarketplace.service_marketplace.dto;

import java.util.List;

//Add more fields in the future
public class UserProfile {
    
    private String email;

    private String firstName;

    private String lastName;

    private String major;

    private String campus;

    private String bio;

    private List<ServiceDto> services;

    public UserProfile(String email, String firstName, String lastName, String major, String campus, String bio, List<ServiceDto> services){
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.major = major;
        this.campus = campus;
        this.bio = bio;
        this.services = services == null ? List.of() : services;
    }

    public String getEmail(){
        return this.email;
    }

    public String getMajor(){
        return this.major;
    }

    public String getFirstName(){
        return this.firstName;
    }

    public String getLastName(){
        return this.lastName;
    }

    public String getCampus(){
        return this.campus;
    }

    public String getBio(){
        return this.bio;
    }

    public List<ServiceDto> getServices(){
        return this.services;
    }
}
