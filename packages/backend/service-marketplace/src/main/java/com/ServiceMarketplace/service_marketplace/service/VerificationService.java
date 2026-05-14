package com.ServiceMarketplace.service_marketplace.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ServiceMarketplace.service_marketplace.dto.ResendResponse;
import com.ServiceMarketplace.service_marketplace.dto.VerificationRequest;
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

    private final PasswordEncoder passwordEncoder;

    public VerificationService(VerificationRepository verificationRepository, UserRepository userRepository, EmailService emailService, PasswordEncoder passwordEncoder){
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void createVerification(String email, String code){

        Verification verification = new Verification(email, passwordEncoder.encode(code));

        verificationRepository.save(verification);

    }

    public VerifiedResponse verifyCode(VerificationRequest request, String email){
        var verification = verificationRepository.findByEmail(email)
            .orElseThrow(()-> new VerificationNotFound());

        if (LocalDateTime.now().isAfter(verification.getExpiryDate())){
            throw new VerificationCodeExpired();
        }

        if (!passwordEncoder.matches(request.getCode(), verification.getVerificationCode())){
            throw new InvalidVerificationCode();
        }

        var user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        user.setVerificationStatus(true);

        userRepository.save(user);

        verificationRepository.delete(verification);

        return new VerifiedResponse("Account verified successfully. You can now log in.", true, LocalDateTime.now());
        
    }

    public ResendResponse resendCode(VerificationRequest request, String email){

        String newCode = generateVerificationCode();

        var verification = verificationRepository.findByEmail(email).orElseThrow(() -> new VerificationNotFound());

        verification.setVerificationCode(passwordEncoder.encode(newCode));
        verification.updateExpiryDate();
        verificationRepository.save(verification);

        return new ResendResponse(newCode, email);
    }

    public String generateVerificationCode(){
        SecureRandom secureRandom = new SecureRandom();

        int randomNumber = secureRandom.nextInt(100000);
        
        return String.format("%06d", randomNumber);
    }

}
