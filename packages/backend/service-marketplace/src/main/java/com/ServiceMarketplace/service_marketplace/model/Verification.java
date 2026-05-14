package com.ServiceMarketplace.service_marketplace.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Document(collection = "verification")
public class Verification {

    private static final int EXPIRATION_MINUTES = 10;

    @Id
    private String id;

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

    public void updateExpiryDate(){
        this.expiryDate = LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES);
    }

}
