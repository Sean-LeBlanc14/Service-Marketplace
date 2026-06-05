package com.ServiceMarketplace.service_marketplace.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ServiceMarketplace.service_marketplace.dto.SupportRequest;
import com.ServiceMarketplace.service_marketplace.dto.SupportResponse;
import com.ServiceMarketplace.service_marketplace.service.SupportService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/support")
public class SupportController {

    private final SupportService supportService;

    public SupportController(SupportService supportService){
        this.supportService = supportService;
    }
    
    @PostMapping("/bug")
    public ResponseEntity<SupportResponse> reportbug(@AuthenticationPrincipal UserDetails userDetails,@Valid @RequestBody SupportRequest request) {
        
        SupportResponse response = supportService.reportBug(userDetails, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/contact")
    public ResponseEntity<SupportResponse> contactSupport(@AuthenticationPrincipal UserDetails userDetails,@Valid @RequestBody SupportRequest request) {
        
        SupportResponse response = supportService.contactSupport(userDetails, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    
}
