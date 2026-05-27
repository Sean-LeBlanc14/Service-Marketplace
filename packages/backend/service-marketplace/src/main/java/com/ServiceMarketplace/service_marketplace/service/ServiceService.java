package com.ServiceMarketplace.service_marketplace.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.ServiceMarketplace.service_marketplace.dto.CreateServiceRequest;
import com.ServiceMarketplace.service_marketplace.dto.ServiceDto;
import com.ServiceMarketplace.service_marketplace.dto.UpdateServiceRequest;
import com.ServiceMarketplace.service_marketplace.exception.ResourceNotFoundException;
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
        var user = getCurrentUser(userDetails);

        Service service = new Service();
        applyServiceFields(
            service,
            request.getTitle(),
            request.getDescription(),
            request.getPriceMin(),
            request.getPriceMax(),
            request.getPriceUnit(),
            request.getCategory(),
            request.getLocation(),
            request.getTags()
        );
        service.setUserId(user.getId());
        service.setCreatedAt(Instant.now());

        return toDto(serviceRepository.save(service), user);
    }

    public ServiceDto updateService(String serviceId, UpdateServiceRequest request, UserDetails userDetails) {
        var user = getCurrentUser(userDetails);
        var service = getOwnedService(serviceId, user);

        applyServiceFields(
            service,
            request.getTitle(),
            request.getDescription(),
            request.getPriceMin(),
            request.getPriceMax(),
            request.getPriceUnit(),
            request.getCategory(),
            request.getLocation(),
            request.getTags()
        );

        return toDto(serviceRepository.save(service), user);
    }

    public void deleteService(String serviceId, UserDetails userDetails) {
        var user = getCurrentUser(userDetails);
        var service = getOwnedService(serviceId, user);

        serviceRepository.delete(service);
    }

    private void applyServiceFields(
        Service service,
        String title,
        String description,
        BigDecimal priceMin,
        BigDecimal priceMax,
        String priceUnit,
        String category,
        String location,
        List<String> tags) {
        service.setTitle(clean(title));
        service.setDescription(clean(description));
        service.setPriceMin(priceMin);
        service.setPriceMax(priceMax);
        service.setPriceUnit(clean(priceUnit));
        service.setCategory(clean(category));
        service.setLocation(clean(location));
        service.setTags(normalizeTags(tags));
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private Service getOwnedService(String serviceId, User user) {
        var service = serviceRepository.findById(clean(serviceId))
            .orElseThrow(() -> new ResourceNotFoundException("Service", serviceId));

        if (!clean(service.getUserId()).equals(user.getId())) {
            throw new AccessDeniedException("You can only modify your own services.");
        }

        return service;
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }

        return tags.stream()
            .map(this::normalizeTag)
            .filter(tag -> !tag.isBlank())
            .collect(Collectors.toList());
    }

    private String normalizeTag(String tag) {
        String normalizedTag = clean(tag).replaceAll("\\s+", " ");

        if (normalizedTag.isBlank()) {
            return "";
        }

        String lowerCaseTag = normalizedTag.toLowerCase(Locale.ROOT);
        StringBuilder titleCaseTag = new StringBuilder(lowerCaseTag.length());
        boolean capitalizeNext = true;

        for (int i = 0; i < lowerCaseTag.length(); i++) {
            char current = lowerCaseTag.charAt(i);

            if (Character.isWhitespace(current)) {
                titleCaseTag.append(current);
                capitalizeNext = true;
                continue;
            }

            if (capitalizeNext && Character.isLetter(current)) {
                titleCaseTag.append(Character.toTitleCase(current));
            } else {
                titleCaseTag.append(current);
            }

            capitalizeNext = false;
        }

        return titleCaseTag.toString();
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
