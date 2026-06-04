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
import com.ServiceMarketplace.service_marketplace.dto.ChangePasswordRequest;
import com.ServiceMarketplace.service_marketplace.dto.DeleteAccountRequest;
import com.ServiceMarketplace.service_marketplace.dto.LoginRequest;
import com.ServiceMarketplace.service_marketplace.dto.ProviderProfile;
import com.ServiceMarketplace.service_marketplace.dto.RegisterRequest;
import com.ServiceMarketplace.service_marketplace.dto.ServiceDto;
import com.ServiceMarketplace.service_marketplace.dto.UpdateUserProfileRequest;
import com.ServiceMarketplace.service_marketplace.dto.UserProfile;
import com.ServiceMarketplace.service_marketplace.exception.EmailAlreadyExistsException;
import com.ServiceMarketplace.service_marketplace.exception.FailedToDeleteUserException;
import com.ServiceMarketplace.service_marketplace.exception.RedundantChangeException;
import com.ServiceMarketplace.service_marketplace.exception.InvalidEmailDomainException;
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
        if (!request.getEmail().endsWith("@calpoly.edu")) {
            throw new InvalidEmailDomainException("Registration is limited to Cal Poly email addresses.");
        }

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

    public AuthResponse loginUser(LoginRequest request) {

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

    public void changeUserPassword(UserDetails userDetails, ChangePasswordRequest request) {


        var user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));


        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid Password");
        }
        
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())){
            throw new RedundantChangeException("Your new password cannot be the same as your old password.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

    }

    public UserProfile getUserProfile(UserDetails userDetails) {

        User user = getUserByEmail(userDetails.getUsername());

        return toUserProfile(user);

    }

    public ProviderProfile getProviderProfile(String userId) {
        User user = getUserById(userId);

        var ratingSummary = serviceService.getProviderRatingSummary(user.getId());

        return new ProviderProfile(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getMajor(),
            user.getCampus(),
            clean(user.getBio()),
            ratingSummary.averageRating(),
            ratingSummary.reviewCount(),
            getProfileServices(user.getId())
        );
    }

    public void deleteUserProfile(DeleteAccountRequest request) {

        try{
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(), request.getPassword()));
        }catch(BadCredentialsException | InternalAuthenticationServiceException e) {
            throw new BadCredentialsException("Invalid email or password.");
        }
        
        var user = userRepository.deleteByEmail(request.getEmail())
                .orElseThrow(() -> new FailedToDeleteUserException("Could not delete the user: " + request.getEmail()));

    }

    public UserProfile updateUserProfile(UserDetails userDetails, UpdateUserProfileRequest request) {

        User user = getUserByEmail(userDetails.getUsername());

        if (request.getBio() != null) {
            user.setBio(clean(request.getBio()));
        }

        User saved = userRepository.save(user);

        return toUserProfile(saved);

    }

    public UserProfile changeUserMajor(UserDetails userDetails, String newMajor){
        var user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.getMajor().equals(newMajor)) throw new RedundantChangeException("Your new major cannot be the same as your old major");
        user.setMajor(newMajor);
        userRepository.save(user);
        return toUserProfile(user);
    }

    public UserProfile changeUserCampus(UserDetails userDetails, String newCampus){
        var user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
                if (user.getCampus().equals(newCampus)) throw new RedundantChangeException("Your new campus cannot be the same as your old campus"); 
        user.setCampus(newCampus);
        userRepository.save(user);
        return toUserProfile(user);
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

        if ("admin".equals(user.getRole())) {
            throw new AccessDeniedException("Cannot unsuspend another admin.");
        }

        if ("suspended".equals(user.getRole())) {
            user.setRole("user");
            return userRepository.save(user);
        }

        return user;
    }

    private UserProfile toUserProfile(User user) {
        var ratingSummary = serviceService.getProviderRatingSummary(user.getId());

        return new UserProfile(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getMajor(),
            user.getCampus(), clean(user.getBio()), user.getVerificationStatus(), user.getRole(),
            ratingSummary.averageRating(), ratingSummary.reviewCount(),
            getProfileServices(user.getId()));
    }

    private List<ServiceDto> getProfileServices(String userId) {
        return serviceService.getServicesByUserId(userId);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

}
