package com.ServiceMarketplace.service_marketplace;

import com.ServiceMarketplace.service_marketplace.dto.CreateServiceRequest;
import com.ServiceMarketplace.service_marketplace.dto.ServiceDto;
import com.ServiceMarketplace.service_marketplace.model.Service;
import com.ServiceMarketplace.service_marketplace.model.User;
import com.ServiceMarketplace.service_marketplace.repository.ServiceRepository;
import com.ServiceMarketplace.service_marketplace.repository.UserRepository;
import com.ServiceMarketplace.service_marketplace.service.ServiceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServiceServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private UserRepository userRepository;

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
        User user = new User();
        user.setId("user123");
        user.setFirstName("Avery");
        user.setLastName("Chen");

        when(serviceRepository.findAll()).thenReturn(List.of(s1, s2));
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));

        List<ServiceDto> result = serviceService.getAllServices();

        assertEquals(2, result.size());
        assertEquals("1", result.get(0).getId());
        assertEquals("2", result.get(1).getId());
        assertEquals("Avery Chen", result.get(0).getProviderName());
        assertEquals("Avery Chen", result.get(1).getProviderName());
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

    @Test
    void getServicesByUserId_returnsUserServices() {
        Service s1 = createMockService("1", "tutoring");

        when(serviceRepository.findByUserId("user123")).thenReturn(List.of(s1));

        List<ServiceDto> result = serviceService.getServicesByUserId("user123");

        assertEquals(1, result.size());
        assertEquals("user123", result.get(0).getUserId());
        verify(serviceRepository).findByUserId("user123");
    }

    @Test
    void createService_savesRequestedFields() {
        CreateServiceRequest request = new CreateServiceRequest();
        request.setTitle(" Calculus tutoring ");
        request.setCategory("tutoring");
        request.setPriceMin(new BigDecimal("20.00"));
        request.setPriceMax(new BigDecimal("50.00"));
        request.setPriceUnit(" ");
        request.setDescription(" Help with derivatives ");
        request.setLocation(" Library ");
        request.setTags(List.of(" math ", "", "calculus"));

        User user = new User();
        user.setId("user123");
        user.setEmail("student@example.com");
        user.setFirstName("Avery");
        user.setLastName("Chen");

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("student@example.com");
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(serviceRepository.save(any(Service.class))).thenAnswer(invocation -> {
            Service service = invocation.getArgument(0);
            service.setId("service123");
            return service;
        });

        ServiceDto result = serviceService.createService(request, userDetails);

        ArgumentCaptor<Service> savedService = ArgumentCaptor.forClass(Service.class);
        verify(serviceRepository).save(savedService.capture());

        assertEquals("service123", result.getId());
        assertEquals("Avery Chen", result.getProviderName());
        assertEquals("Calculus tutoring", savedService.getValue().getTitle());
        assertEquals("tutoring", savedService.getValue().getCategory());
        assertEquals(new BigDecimal("20.00"), savedService.getValue().getPriceMin());
        assertEquals(new BigDecimal("50.00"), savedService.getValue().getPriceMax());
        assertEquals("", savedService.getValue().getPriceUnit());
        assertEquals("Help with derivatives", savedService.getValue().getDescription());
        assertEquals("Library", savedService.getValue().getLocation());
        assertEquals(List.of("math", "calculus"), savedService.getValue().getTags());
        assertEquals("user123", savedService.getValue().getUserId());
    }
}
