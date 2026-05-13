package com.ServiceMarketplace.service_marketplace.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection = "users")
public class User {
    
    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String password;

    private String firstName;

    private String lastName;

    private String major;

    private String campus;

    private String bio = "";

    private List<UserServiceListing> services = new ArrayList<>();

    private boolean isVerified = false;

    @CreatedDate
    private Instant createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName(){
        return this.firstName;
    }

    public void setFirstName(String firstName){
        this.firstName = firstName;
    }

    public String getLastName(){
        return this.lastName;
    }

    public void setLastName(String lastName){
        this.lastName = lastName;
    }

    public String getMajor(){
        return this.major;
    }

    public void setMajor(String major){
        this.major = major;
    }

    public String getCampus(){
        return this.campus;
    }

    public void setCampus(String campus){
        this.campus = campus;
    }

    public String getBio(){
        return this.bio;
    }

    public void setBio(String bio){
        this.bio = bio;
    }

    public List<UserServiceListing> getServices(){
        return this.services;
    }

    public void setServices(List<UserServiceListing> services){
        this.services = services == null ? new ArrayList<>() : services;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setVerificationStatus(boolean status){
        this.isVerified = status;
    }

    public boolean getVerificationStatus(){
        return this.isVerified;
    }

    

}
