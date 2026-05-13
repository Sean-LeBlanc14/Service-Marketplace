package com.ServiceMarketplace.service_marketplace.service;

import java.util.List;
import java.util.stream.Collectors;

import com.ServiceMarketplace.service_marketplace.dto.ServiceDto;
import com.ServiceMarketplace.service_marketplace.model.Service;
import com.ServiceMarketplace.service_marketplace.repository.ServiceRepository;

@org.springframework.stereotype.Service
public class ServiceService {

    private final ServiceRepository serviceRepository;

    public ServiceService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    private ServiceDto toDto(Service service) {
        return new ServiceDto(
            service.getId(),
            service.getTitle(),
            service.getCategory(),
            service.getUserId(),
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
}