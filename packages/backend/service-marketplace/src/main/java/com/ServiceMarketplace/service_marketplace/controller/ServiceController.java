package com.ServiceMarketplace.service_marketplace.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ServiceMarketplace.service_marketplace.dto.CreateServiceRequest;
import com.ServiceMarketplace.service_marketplace.dto.ServiceDto;
import com.ServiceMarketplace.service_marketplace.dto.UpdateServiceRequest;
import com.ServiceMarketplace.service_marketplace.model.User;
import com.ServiceMarketplace.service_marketplace.repository.ServiceRepository;
import com.ServiceMarketplace.service_marketplace.repository.UserRepository;
import com.ServiceMarketplace.service_marketplace.service.ServiceService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceService serviceService;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;

    public ServiceController(
            ServiceService serviceService,
            ServiceRepository serviceRepository,
            UserRepository userRepository) {
        this.serviceService = serviceService;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<ServiceDto>> getAllServices() {
        List<ServiceDto> services = serviceService.getAllServices();
        return ResponseEntity.status(HttpStatus.OK).body(services);
    }

    @PostMapping
    public ResponseEntity<ServiceDto> createService(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateServiceRequest request) {
        ServiceDto service = serviceService.createService(request, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(service);
    }

    @PutMapping("/{serviceId}")
    public ResponseEntity<ServiceDto> updateService(
            @PathVariable String serviceId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateServiceRequest request) {
        ServiceDto service = serviceService.updateService(serviceId, request, userDetails);
        return ResponseEntity.status(HttpStatus.OK).body(service);
    }

    @DeleteMapping("/{serviceId}")
    public ResponseEntity<Void> deleteService(
            @PathVariable String serviceId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User requester = getCurrentUser(userDetails);

        if (!isAdmin(requester)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        var service = serviceRepository.findById(serviceId);

        if (service.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        serviceRepository.delete(service.get());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private boolean isAdmin(User user) {
        return "admin".equals(user.getRole());
    }
}
