package com.ServiceMarketplace.service_marketplace.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
import com.ServiceMarketplace.service_marketplace.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
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

    /*This can be temp, I need the user's name for the booking card, we can possibly add the names to the booking model
    then add them to the booking response*/
    @GetMapping("/profile/{id}")
    public ResponseEntity<UserProfile> getUser(@PathVariable String id) {
        UserProfile user = userService.getUserById(id);

        return ResponseEntity.status(HttpStatus.OK).body(user);
    }
    

    @GetMapping
    public ResponseEntity<List<User>> getUsers(@AuthenticationPrincipal UserDetails userDetails) {
        if (!userService.isAdmin(userDetails.getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.status(HttpStatus.OK).body(userService.getAllUsers());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserById(
            @PathVariable String userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (!userService.isAdmin(userDetails.getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.status(HttpStatus.OK).body(userService.getUserById(userId));
    }

    @PutMapping("/{userId}/suspend")
    public ResponseEntity<User> suspendUser(
            @PathVariable String userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (!userService.isAdmin(userDetails.getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.status(HttpStatus.OK).body(userService.suspendUser(userId));
    }

    @PutMapping("/{userId}/unsuspend")
    public ResponseEntity<User> unsuspendUser(
            @PathVariable String userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (!userService.isAdmin(userDetails.getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.status(HttpStatus.OK).body(userService.unsuspendUser(userId));
    }
}
