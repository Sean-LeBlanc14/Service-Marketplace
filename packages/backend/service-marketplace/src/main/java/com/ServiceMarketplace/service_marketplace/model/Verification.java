package com.ServiceMarketplace.service_marketplace.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;

@Document(collection = "verification")
public class Verification {

    private static final int EXPIRATION_MINUTES = 10;

    @Id
    private String Id;

    @Indexed(unique = true)
    @NotBlank
    private String email;

    @NotBlank
    private String verificationCode;

    private LocalDateTime expiryDate;

    public Verification(String email, String verificationCode){
        this.email = email;
        this.verificationCode = verificationCode;
        this.expiryDate = LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES);
    }

    public String getEmail(){
        return this.email;
    }

    public String getVerificationCode(){
        return this.verificationCode;
    }

    public LocalDateTime getExpiryDate(){
        return this.expiryDate;
    }



    
}
