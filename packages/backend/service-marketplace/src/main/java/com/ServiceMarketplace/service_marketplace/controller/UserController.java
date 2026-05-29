package com.ServiceMarketplace.service_marketplace.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ServiceMarketplace.service_marketplace.dto.UpdateUserProfileRequest;
import com.ServiceMarketplace.service_marketplace.dto.UserProfile;
import com.ServiceMarketplace.service_marketplace.model.User;
import com.ServiceMarketplace.service_marketplace.repository.UserRepository;
import com.ServiceMarketplace.service_marketplace.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository){
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfile> getUserProfile(@AuthenticationPrincipal UserDetails userDetails) {

        UserProfile response = userService.getUserProfile(userDetails);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/me")
    public ResponseEntity<UserProfile> updateUserProfile(@AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody UpdateUserProfileRequest request) {

        UserProfile response = userService.updateUserProfile(userDetails, request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping
    public ResponseEntity<List<User>> getUsers(@AuthenticationPrincipal UserDetails userDetails) {
        User requester = getCurrentUser(userDetails);

        if (!isAdmin(requester)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.status(HttpStatus.OK).body(userRepository.findAll());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserById(
            @PathVariable String userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User requester = getCurrentUser(userDetails);

        if (!isAdmin(requester)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        var user = userRepository.findById(userId);

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.status(HttpStatus.OK).body(user.get());
    }

    @PutMapping("/{userId}/suspend")
    public ResponseEntity<?> suspendUser(
            @PathVariable String userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User requester = getCurrentUser(userDetails);

        if (!isAdmin(requester)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        var userToSuspend = userRepository.findById(userId);

        if (userToSuspend.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        User user = userToSuspend.get();

        if ("admin".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cannot suspend another admin.");
        }

        user.setRole("suspended");

        return ResponseEntity.status(HttpStatus.OK).body(userRepository.save(user));
    }

    @PutMapping("/{userId}/unsuspend")
    public ResponseEntity<User> unsuspendUser(
            @PathVariable String userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User requester = getCurrentUser(userDetails);

        if (!isAdmin(requester)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        var userToUnsuspend = userRepository.findById(userId);

        if (userToUnsuspend.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        User user = userToUnsuspend.get();
        user.setRole("user");

        return ResponseEntity.status(HttpStatus.OK).body(userRepository.save(user));
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private boolean isAdmin(User user) {
        return "admin".equals(user.getRole());
    }
}
