package com.ServiceMarketplace.service_marketplace;

import com.ServiceMarketplace.service_marketplace.dto.ServiceDto;
import com.ServiceMarketplace.service_marketplace.model.Service;
import com.ServiceMarketplace.service_marketplace.repository.ServiceRepository;
import com.ServiceMarketplace.service_marketplace.service.ServiceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServiceServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private ServiceService serviceService;

    private Service createMockService(String id, String category) {
        Service service = new Service();
        service.setId(id);
        service.setTitle("Test Service");
        service.setCategory(category);
        service.setUserId("user123");
        service.setPriceMin(new BigDecimal("10.00"));
        service.setPriceMax(new BigDecimal("50.00"));
        service.setPriceUnit("hr");
        service.setDescription("Test description");
        service.setLocation("On campus");
        service.setTags(List.of("tag1", "tag2"));
        return service;
    }

    @Test
    void getAllServices_returnsAllServices() {
        Service s1 = createMockService("1", "tutoring");
        Service s2 = createMockService("2", "tech");

        when(serviceRepository.findAll()).thenReturn(List.of(s1, s2));

        List<ServiceDto> result = serviceService.getAllServices();

        assertEquals(2, result.size());
        assertEquals("1", result.get(0).getId());
        assertEquals("2", result.get(1).getId());
        verify(serviceRepository).findAll();
    }

    @Test
    void getAllServices_emptyList_returnsEmpty() {
        when(serviceRepository.findAll()).thenReturn(List.of());

        List<ServiceDto> result = serviceService.getAllServices();

        assertEquals(0, result.size());
        verify(serviceRepository).findAll();
    }

    @Test
    void getServicesByCategory_returnsMatchingServices() {
        Service s1 = createMockService("1", "tutoring");

        when(serviceRepository.findByCategory("tutoring")).thenReturn(List.of(s1));

        List<ServiceDto> result = serviceService.getServicesByCategory("tutoring");

        assertEquals(1, result.size());
        assertEquals("tutoring", result.get(0).getCategory());
        verify(serviceRepository).findByCategory("tutoring");
    }

    @Test
    void getServicesByCategory_noMatch_returnsEmpty() {
        when(serviceRepository.findByCategory("nonexistent")).thenReturn(List.of());

        List<ServiceDto> result = serviceService.getServicesByCategory("nonexistent");

        assertEquals(0, result.size());
        verify(serviceRepository).findByCategory("nonexistent");
    }
}