package com.ServiceMarketplace.service_marketplace.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ServiceMarketplace.service_marketplace.dto.VerificationRequest;
import com.ServiceMarketplace.service_marketplace.service.VerificationService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/verification")
public class VerificationController {

    @Autowired
    private VerificationService verificationService;

    @PostMapping("/code")
    public ResponseEntity<Boolean> verifyCode(@Valid @RequestBody VerificationRequest request) {
        
        boolean verified = verificationService.verifyCode(request);

        return ResponseEntity.status(HttpStatus.OK).body(verified);
    }
    
    
}
