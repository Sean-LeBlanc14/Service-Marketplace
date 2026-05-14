package com.ServiceMarketplace.service_marketplace.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ServiceMarketplace.service_marketplace.dto.AuthResponse;
import com.ServiceMarketplace.service_marketplace.dto.LoginRequest;
import com.ServiceMarketplace.service_marketplace.dto.RegisterRequest;
import com.ServiceMarketplace.service_marketplace.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {

        AuthResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request){

        AuthResponse response = userService.loginUser(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String,String>> logout(HttpServletRequest request, HttpServletResponse response) {
        
        String res = userService.logoutUser(request, response);

        Map<String, String> body = new HashMap<>();

        body.put("Message", res);

        return ResponseEntity.status(HttpStatus.OK).body(body);
    }
    
    
}