package com.ServiceMarketplace.service_marketplace.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ServiceMarketplace.service_marketplace.dto.VerificationRequest;
import com.ServiceMarketplace.service_marketplace.dto.VerificationResponse;
import com.ServiceMarketplace.service_marketplace.dto.VerifiedResponse;
import com.ServiceMarketplace.service_marketplace.exception.InvalidVerificationCode;
import com.ServiceMarketplace.service_marketplace.exception.VerificationCodeExpired;
import com.ServiceMarketplace.service_marketplace.exception.VerificationNotFound;
import com.ServiceMarketplace.service_marketplace.model.Verification;
import com.ServiceMarketplace.service_marketplace.repository.UserRepository;
import com.ServiceMarketplace.service_marketplace.repository.VerificationRepository;

@Service
public class VerificationService {
    
    private final VerificationRepository verificationRepository;

    private final UserRepository userRepository;

    public VerificationService(VerificationRepository verificationRepository, UserRepository userRepository, EmailService emailService){
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
    }

    public void createVerification(String email, String code){

        Verification verification = new Verification(email, code);

        verificationRepository.save(verification);

    }

    public VerifiedResponse verifyCode(VerificationRequest request, String email){
        var verification = verificationRepository.findByEmail(email)
            .orElseThrow(()-> new VerificationNotFound());

        if (LocalDateTime.now().isAfter(verification.getExpiryDate())){
            throw new VerificationCodeExpired();
        }

        if (!request.getCode().equals(verification.getVerificationCode())){
            throw new InvalidVerificationCode();
        }

        var user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        user.setVerificationStatus(true);

        userRepository.save(user);

        verificationRepository.delete(verification);

        return new VerifiedResponse("Account verified successfully. You can now log in.", true, LocalDateTime.now());
        
    }

    public VerificationResponse resendCode(VerificationRequest request, String email){

        String newCode = generateVerificationCode();

        var verification = verificationRepository.findByEmail(newCode).orElseThrow(() -> new VerificationNotFound());

        verification.setVerificationCode(newCode);
        verification.updateExpiryDate();
        verificationRepository.save(verification);

        return new VerificationResponse(email, newCode);
    }

    public String generateVerificationCode(){
        SecureRandom secureRandom = new SecureRandom();

        int randomNumber = secureRandom.nextInt(100000);
        
        return String.format("%06d", randomNumber);
    }

    public void delete(){
        verificationRepository.deleteAll();
    }

}
