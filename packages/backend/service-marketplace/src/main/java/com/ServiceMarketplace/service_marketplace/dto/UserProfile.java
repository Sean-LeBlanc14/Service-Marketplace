package com.ServiceMarketplace.service_marketplace.dto;
//Add more fields in the future
public class UserProfile {
    
    private String email;

    private String firstName;

    private String lastName;

    private String major;

    private String campus;

    public UserProfile(String email, String firstName, String lastName, String major, String campus){
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.major = major;
        this.campus = campus;
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
}
