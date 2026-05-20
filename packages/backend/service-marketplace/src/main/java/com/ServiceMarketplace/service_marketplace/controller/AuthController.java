package com.ServiceMarketplace.service_marketplace.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ServiceMarketplace.service_marketplace.dto.AuthResponse;
import com.ServiceMarketplace.service_marketplace.dto.LoginRequest;
import com.ServiceMarketplace.service_marketplace.dto.LogoutResponse;
import com.ServiceMarketplace.service_marketplace.dto.RegisterRequest;
import com.ServiceMarketplace.service_marketplace.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    private final SecurityContextLogoutHandler securityContextLogoutHandler;

    public AuthController(UserService userService, SecurityContextLogoutHandler securityContextLogoutHandler) {
        this.userService = userService;
        this.securityContextLogoutHandler = securityContextLogoutHandler;
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
    public ResponseEntity<LogoutResponse> logout(HttpServletRequest request, HttpServletResponse response) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null){
            securityContextLogoutHandler.logout(request, response, auth);
        }else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new LogoutResponse("Failed to logout, no auth details"));
        }

        return ResponseEntity.status(HttpStatus.OK).body(new LogoutResponse("Success"));
    }
    
    
}