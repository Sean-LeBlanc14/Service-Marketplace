package com.ServiceMarketplace.service_marketplace.service;

import org.springframework.stereotype.Service;

import com.ServiceMarketplace.service_marketplace.model.User;
import com.ServiceMarketplace.service_marketplace.repository.UserRepository;

@Service
public class UserService {
    
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(User user) {
        // TODO: VALIDATE EMAIL, HASH PASSWORD, CHECK FOR DUPLICATES
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
