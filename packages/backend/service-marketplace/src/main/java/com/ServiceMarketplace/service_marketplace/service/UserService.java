package com.ServiceMarketplace.service_marketplace.service;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
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
import com.ServiceMarketplace.service_marketplace.exception.ResourceNotFoundException;
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

        return new AuthResponse(saved.getId(), saved.getEmail(), jwtToken, saved.getRole());
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

        User user = getUserByEmail(request.getEmail());

        String jwtToken = jwtService.generateToken(request.getEmail());

        return new AuthResponse(user.getId(), user.getEmail(), jwtToken, user.getRole());

    }

    public UserProfile getUserProfile(UserDetails userDetails){

        User user = getUserByEmail(userDetails.getUsername());

        return toUserProfile(user);
        
    }

    public UserProfile updateUserProfile(UserDetails userDetails, UpdateUserProfileRequest request){

        User user = getUserByEmail(userDetails.getUsername());

        if (request.getBio() != null) {
            user.setBio(clean(request.getBio()));
        }

        User saved = userRepository.save(user);

        return toUserProfile(saved);
        
    }

    public User getUserById(String userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public boolean isAdmin(String email) {
        return "admin".equals(getUserByEmail(email).getRole());
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User suspendUser(String userId) {
        User user = getUserById(userId);

        if ("admin".equals(user.getRole())) {
            throw new AccessDeniedException("Cannot suspend another admin.");
        }

        user.setRole("suspended");

        return userRepository.save(user);
    }

    public User unsuspendUser(String userId) {
        User user = getUserById(userId);
        user.setRole("user");

        return userRepository.save(user);
    }

    private UserProfile toUserProfile(User user) {
        return new UserProfile(user.getEmail(), user.getFirstName(), user.getLastName(), user.getMajor(),
            user.getCampus(), clean(user.getBio()), user.getVerificationStatus(), user.getRole(),
            getProfileServices(user.getId()));
    }

    private List<ServiceDto> getProfileServices(String userId) {
        return serviceService.getServicesByUserId(userId);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

}
