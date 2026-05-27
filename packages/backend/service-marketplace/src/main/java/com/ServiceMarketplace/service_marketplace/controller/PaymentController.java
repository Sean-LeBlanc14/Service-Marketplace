package com.ServiceMarketplace.service_marketplace.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ServiceMarketplace.service_marketplace.dto.ConnectOnboardingResponse;
import com.ServiceMarketplace.service_marketplace.dto.ConnectStatusResponse;
import com.ServiceMarketplace.service_marketplace.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        paymentService.handleWebhook(payload, sigHeader);
        return new ResponseEntity<>("Webhook received", HttpStatus.OK);
    }

    @PostMapping("/connect")
    public ResponseEntity<ConnectOnboardingResponse> initiateOnboarding(
            @AuthenticationPrincipal UserDetails userDetails) {
        ConnectOnboardingResponse response = paymentService.initiateOnboarding(userDetails);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/connect/status")
    public ResponseEntity<ConnectStatusResponse> getConnectStatus(
            @AuthenticationPrincipal UserDetails userDetails) {
        ConnectStatusResponse response = paymentService.getConnectStatus(userDetails);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
