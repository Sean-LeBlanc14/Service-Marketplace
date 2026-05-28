package com.ServiceMarketplace.service_marketplace.service;

import java.util.List;

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
import com.ServiceMarketplace.service_marketplace.dto.ServiceDto;
import com.ServiceMarketplace.service_marketplace.dto.UpdateUserProfileRequest;
import com.ServiceMarketplace.service_marketplace.dto.UserProfile;
import com.ServiceMarketplace.service_marketplace.exception.EmailAlreadyExistsException;
import com.ServiceMarketplace.service_marketplace.model.User;
import com.ServiceMarketplace.service_marketplace.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final EmailService emailService;

    private final VerificationService verificationService;

    private final ServiceService serviceService;


    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService, 
        VerificationService verificationService, AuthenticationManager authenticationManager, JwtService jwtService,
        ServiceService serviceService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.verificationService = verificationService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.serviceService = serviceService;
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

        var user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String jwtToken = jwtService.generateToken(request.getEmail());

        return new AuthResponse(user.getId(), user.getEmail(), jwtToken);

    }

    public UserProfile getUserProfile(UserDetails userDetails){

        var user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return toUserProfile(user);
        
    }

    public UserProfile getUserById(String id){
        var user = userRepository.findById(id).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return toUserProfile(user);
    }

    public UserProfile updateUserProfile(UserDetails userDetails, UpdateUserProfileRequest request){

        var user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (request.getBio() != null) {
            user.setBio(clean(request.getBio()));
        }

        User saved = userRepository.save(user);

        return toUserProfile(saved);
        
    }

    private UserProfile toUserProfile(User user) {
        return new UserProfile(user.getEmail(), user.getFirstName(), user.getLastName(), user.getMajor(),
            user.getCampus(), clean(user.getBio()), user.getVerificationStatus(),getProfileServices(user.getId()));
    }

    private List<ServiceDto> getProfileServices(String userId) {
        return serviceService.getServicesByUserId(userId);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

}