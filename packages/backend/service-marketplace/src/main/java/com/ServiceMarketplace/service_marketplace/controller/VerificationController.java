package com.ServiceMarketplace.service_marketplace.controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ServiceMarketplace.service_marketplace.dto.ResendResponse;
import com.ServiceMarketplace.service_marketplace.dto.VerificationRequest;
import com.ServiceMarketplace.service_marketplace.dto.VerifiedResponse;
import com.ServiceMarketplace.service_marketplace.service.EmailService;
import com.ServiceMarketplace.service_marketplace.service.JwtService;
import com.ServiceMarketplace.service_marketplace.service.VerificationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/verification")
public class VerificationController {

    private VerificationService verificationService;

    private EmailService emailService;

    private JwtService jwtService;

    public VerificationController(VerificationService verificationService, EmailService emailService, JwtService jwtService){
        this.verificationService = verificationService;
        this.emailService = emailService;
        this.jwtService = jwtService;
    }

    @PostMapping("/code")
    public ResponseEntity<VerifiedResponse> verifyCode(@Valid @RequestBody VerificationRequest request, HttpServletRequest servletRequest) {
        
        String auth = servletRequest.getHeader("Authorization");

        String email = jwtService.extractEmail(auth.substring(7));

        VerifiedResponse verified = verificationService.verifyCode(request, email);

        return ResponseEntity.status(HttpStatus.OK).body(verified);
    }

    @PutMapping("/resend")
    public ResponseEntity<ResendResponse> resendCode(@Valid @RequestBody VerificationRequest request,  HttpServletRequest servletRequest) {

        String auth = servletRequest.getHeader("Authorization");

        String email = jwtService.extractEmail(auth.substring(7));

        ResendResponse response = verificationService.resendCode(request, email);

        emailService.sendVerificationEmail(response.getEmail(), response.getCode());
        
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
