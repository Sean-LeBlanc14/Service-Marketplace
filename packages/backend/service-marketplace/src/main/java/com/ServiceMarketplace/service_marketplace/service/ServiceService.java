package com.ServiceMarketplace.service_marketplace.service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.ServiceMarketplace.service_marketplace.dto.CreateServiceRequest;
import com.ServiceMarketplace.service_marketplace.dto.ServiceDto;
import com.ServiceMarketplace.service_marketplace.model.Service;
import com.ServiceMarketplace.service_marketplace.model.User;
import com.ServiceMarketplace.service_marketplace.repository.ServiceRepository;
import com.ServiceMarketplace.service_marketplace.repository.UserRepository;

@org.springframework.stereotype.Service
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;

    public ServiceService(ServiceRepository serviceRepository, UserRepository userRepository) {
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
    }

    private ServiceDto toDto(Service service) {
        return toDto(service, getProviderName(service.getUserId()));
    }

    private ServiceDto toDto(Service service, User provider) {
        return toDto(service, getDisplayName(provider));
    }

    private ServiceDto toDto(Service service, String providerName) {
        return new ServiceDto(
            service.getId(),
            service.getTitle(),
            service.getCategory(),
            service.getUserId(),
            providerName,
            service.getPriceMin(),
            service.getPriceMax(),
            service.getPriceUnit(),
            service.getDescription(),
            service.getLocation(),
            service.getTags()
        );
    }

    public List<ServiceDto> getAllServices() {
        return serviceRepository.findAll()
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public List<ServiceDto> getServicesByCategory(String category) {
        return serviceRepository.findByCategory(category)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public List<ServiceDto> getServicesByUserId(String userId) {
        return serviceRepository.findByUserId(userId)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public ServiceDto createService(CreateServiceRequest request, UserDetails userDetails) {
        var user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Service service = new Service();
        service.setTitle(clean(request.getTitle()));
        service.setDescription(clean(request.getDescription()));
        service.setPriceMin(request.getPriceMin());
        service.setPriceMax(request.getPriceMax());
        service.setPriceUnit(clean(request.getPriceUnit()));
        service.setCategory(clean(request.getCategory()));
        service.setLocation(clean(request.getLocation()));
        service.setTags(normalizeTags(request.getTags()));
        service.setUserId(user.getId());
        service.setCreatedAt(Instant.now());

        return toDto(serviceRepository.save(service), user);
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }

        return tags.stream()
            .map(this::clean)
            .filter(tag -> !tag.isBlank())
            .collect(Collectors.toList());
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String getProviderName(String userId) {
        String cleanUserId = clean(userId);

        if (cleanUserId.isBlank()) {
            return "Service creator";
        }

        return userRepository.findById(cleanUserId)
            .map(this::getDisplayName)
            .orElse("Service creator");
    }

    private String getDisplayName(User user) {
        String fullName = (clean(user.getFirstName()) + " " + clean(user.getLastName())).trim();

        if (!fullName.isBlank()) {
            return fullName;
        }

        String email = clean(user.getEmail());
        return email.isBlank() ? "Service creator" : email;
    }
}
