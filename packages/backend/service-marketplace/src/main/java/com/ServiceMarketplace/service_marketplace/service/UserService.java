package com.ServiceMarketplace.service_marketplace.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ServiceMarketplace.service_marketplace.dto.AuthResponse;
import com.ServiceMarketplace.service_marketplace.dto.LoginRequest;
import com.ServiceMarketplace.service_marketplace.dto.RegisterRequest;
import com.ServiceMarketplace.service_marketplace.dto.UserProfile;
import com.ServiceMarketplace.service_marketplace.exception.EmailAlreadyExistsException;
import com.ServiceMarketplace.service_marketplace.model.User;
import com.ServiceMarketplace.service_marketplace.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    AuthenticationManager authenticationManager;

    JwtService jwtService;
   
    private final EmailService emailService;

    private final VerificationService verificationService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService, 
        VerificationService verificationService, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.verificationService = verificationService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthResponse registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setMajor(request.getMajor());
        user.setCampus(request.getCampus());

        String jwtToken = jwtService.generateToken(request.getEmail());

        String code = verificationService.generateVerificationCode();

        verificationService.createVerification(user.getEmail(), code);

        emailService.sendVerificationEmail(user.getEmail(), code);

        User saved = userRepository.save(user);

        return new AuthResponse(saved.getId(), saved.getEmail(), jwtToken);
    }

    public AuthResponse loginUser(LoginRequest request){
        
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(), 
                    request.getPassword()
                )
            );
        } catch (BadCredentialsException | InternalAuthenticationServiceException e) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        var user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new UsernameNotFoundException("Email not found"));

        String jwtToken = jwtService.generateToken(request.getEmail());

        return new AuthResponse(user.getId(), user.getEmail(), jwtToken);

    }

    public UserProfile getUserProfile(UserDetails userDetails){

        var user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new UserProfile(user.getEmail(), user.getFirstName(), user.getLastName(), user.getMajor(), user.getCampus());
        
    }

}