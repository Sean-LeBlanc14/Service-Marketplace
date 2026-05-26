package com.ServiceMarketplace.service_marketplace;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ServiceMarketplace.service_marketplace.dto.CreateServiceRequest;
import com.ServiceMarketplace.service_marketplace.dto.UpdateServiceRequest;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

public class ServiceRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void createServiceRequest_rejectsTooManyTags() {
        CreateServiceRequest request = validCreateServiceRequest();
        request.setTags(List.of("One", "Two", "Three", "Four", "Five", "Six"));

        assertHasViolation(request, "Use no more than 5 tags");
    }

    @Test
    void createServiceRequest_rejectsLongTitleAndDescription() {
        CreateServiceRequest request = validCreateServiceRequest();
        request.setTitle("a".repeat(CreateServiceRequest.MAX_TITLE_LENGTH + 1));
        request.setDescription("a".repeat(CreateServiceRequest.MAX_DESCRIPTION_LENGTH + 1));

        assertHasViolation(request, "Title must be 80 characters or fewer");
        assertHasViolation(request, "Description must be 1000 characters or fewer");
    }

    @Test
    void updateServiceRequest_rejectsTooManyTags() {
        UpdateServiceRequest request = validUpdateServiceRequest();
        request.setTags(List.of("One", "Two", "Three", "Four", "Five", "Six"));

        assertHasViolation(request, "Use no more than 5 tags");
    }

    private CreateServiceRequest validCreateServiceRequest() {
        CreateServiceRequest request = new CreateServiceRequest();
        request.setTitle("Calculus tutoring");
        request.setDescription("Help with derivatives");
        request.setPriceMin(new BigDecimal("20.00"));
        request.setPriceMax(new BigDecimal("50.00"));
        request.setCategory("tutoring");
        request.setLocation("Library");
        request.setTags(List.of("Math"));
        return request;
    }

    private UpdateServiceRequest validUpdateServiceRequest() {
        UpdateServiceRequest request = new UpdateServiceRequest();
        request.setTitle("Calculus tutoring");
        request.setDescription("Help with derivatives");
        request.setPriceMin(new BigDecimal("20.00"));
        request.setPriceMax(new BigDecimal("50.00"));
        request.setCategory("tutoring");
        request.setLocation("Library");
        request.setTags(List.of("Math"));
        return request;
    }

    private void assertHasViolation(Object request, String message) {
        assertTrue(
            validator.validate(request).stream()
                .anyMatch(violation -> message.equals(violation.getMessage()))
        );
    }
}
