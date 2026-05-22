package com.ServiceMarketplace.service_marketplace.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateServiceRequest {

    @NotBlank
    private String title;

    @NotBlank
    @Size(max = 1000)
    private String description;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal priceMin;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal priceMax;

    @Size(max = 40)
    private String priceUnit;

    @NotBlank
    private String category;

    @NotBlank
    private String location;

    private List<@Size(max = 50) String> tags;

    @AssertTrue(message = "Price max must be greater than or equal to price min")
    public boolean isPriceRangeValid() {
        return priceMin == null || priceMax == null || priceMax.compareTo(priceMin) >= 0;
    }

}
