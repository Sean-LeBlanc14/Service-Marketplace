package com.ServiceMarketplace.service_marketplace.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
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

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean isVerified = false;

    @CreatedDate
    private Instant createdAt;

    public void setVerificationStatus(boolean status){
        this.isVerified = status;
    }

    public boolean getVerificationStatus(){
        return this.isVerified;
    }

    

}
