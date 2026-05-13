package com.ServiceMarketplace.service_marketplace.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import com.ServiceMarketplace.service_marketplace.dto.UpdateUserProfileRequest;
import com.ServiceMarketplace.service_marketplace.dto.UserServiceListingRequest;
import com.ServiceMarketplace.service_marketplace.dto.UserProfile;
import com.ServiceMarketplace.service_marketplace.exception.EmailAlreadyExistsException;
import com.ServiceMarketplace.service_marketplace.model.User;
import com.ServiceMarketplace.service_marketplace.model.UserServiceListing;
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

        var user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String jwtToken = jwtService.generateToken(request.getEmail());

        return new AuthResponse(user.getId(), user.getEmail(), jwtToken);

    }

    public UserProfile getUserProfile(UserDetails userDetails){

        var user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return toUserProfile(user);
        
    }

    public UserProfile updateUserProfile(UserDetails userDetails, UpdateUserProfileRequest request){

        var user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (request.getBio() != null) {
            user.setBio(clean(request.getBio()));
        }

        if (request.getServices() != null) {
            user.setServices(toUserServiceListings(request.getServices()));
        }

        User saved = userRepository.save(user);

        return toUserProfile(saved);
        
    }

    private UserProfile toUserProfile(User user) {
        return new UserProfile(user.getEmail(), user.getFirstName(), user.getLastName(), user.getMajor(),
            user.getCampus(), clean(user.getBio()), user.getServices());
    }

    private List<UserServiceListing> toUserServiceListings(List<UserServiceListingRequest> requests) {
        List<UserServiceListing> services = new ArrayList<>();

        for (UserServiceListingRequest request : requests) {
            UserServiceListing service = new UserServiceListing();
            service.setId(clean(request.getId()));

            if (service.getId().isBlank()) {
                service.setId(UUID.randomUUID().toString());
            }

            service.setTitle(clean(request.getTitle()));
            service.setDescription(clean(request.getDescription()));
            service.setPrice(clean(request.getPrice()));
            service.setIsHourly(request.getIsHourly());
            service.setLocation(clean(request.getLocation()));
            service.setTags(cleanTags(request.getTags()));
            services.add(service);
        }

        return services;
    }

    private List<String> cleanTags(List<String> tags) {
        List<String> cleanTags = new ArrayList<>();

        if (tags == null) {
            return cleanTags;
        }

        for (String tag : tags) {
            String cleanTag = clean(tag);

            if (!cleanTag.isBlank()) {
                cleanTags.add(cleanTag);
            }
        }

        return cleanTags;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

}
