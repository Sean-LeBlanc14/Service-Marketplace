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

    private final VerificationService verificationService;

    private final EmailService emailService;

    private final JwtService jwtService;

    public VerificationController(VerificationService verificationService, EmailService emailService, JwtService jwtService){
        this.verificationService = verificationService;
        this.emailService = emailService;
        this.jwtService = jwtService;
    }

    private String extractEmailFromRequest(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return null;
        }
        return jwtService.extractEmail(auth.substring(7));
    }

    @PostMapping("/code")
    public ResponseEntity<VerifiedResponse> verifyCode(@Valid @RequestBody VerificationRequest request, HttpServletRequest servletRequest) {
        
        String email = extractEmailFromRequest(servletRequest);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        VerifiedResponse verified = verificationService.verifyCode(request, email);

        return ResponseEntity.status(HttpStatus.OK).body(verified);
    }

    @PutMapping("/resend")
    public ResponseEntity<ResendResponse> resendCode(@Valid @RequestBody VerificationRequest request,  HttpServletRequest servletRequest) {

        String email = extractEmailFromRequest(servletRequest);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ResendResponse response = verificationService.resendCode(request, email);

        emailService.sendVerificationEmail(response.getEmail(), response.getCode());
        
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
