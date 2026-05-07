package com.ServiceMarketplace.service_marketplace.service;

import java.time.LocalDateTime;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ServiceMarketplace.service_marketplace.dto.VerificationRequest;
import com.ServiceMarketplace.service_marketplace.exception.InvalidVerificationCode;
import com.ServiceMarketplace.service_marketplace.exception.VerificationCodeExpired;
import com.ServiceMarketplace.service_marketplace.model.Verification;
import com.ServiceMarketplace.service_marketplace.repository.UserRepository;
import com.ServiceMarketplace.service_marketplace.repository.VerificationRepository;

@Service
public class VerificationService {
    
    private final VerificationRepository verificationRepository;

    private final UserRepository userRepository;

    public VerificationService(VerificationRepository verificationRepository, UserRepository userRepository){
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
    }

    public void createVerification(String email, String code){

        Verification verification = new Verification(email, code);

        verificationRepository.save(verification);

    }

    public boolean verifyCode(VerificationRequest request){
        var verification = verificationRepository.findByEmail(request.getEmail()).orElseThrow(()-> new RuntimeException("Verification credentials not found for this user!"));

        if (LocalDateTime.now().isAfter(verification.getExpiryDate())){
            throw new VerificationCodeExpired();
        }

        if (request.getCode().equals(verification.getVerificationCode())){
            verificationRepository.delete(verification);

            var user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new UsernameNotFoundException("User not found"));

            user.setVerificationStatus(true);

            userRepository.save(user);

            return true;
   
        }else{
            throw new InvalidVerificationCode();
        }
        
    }

}
