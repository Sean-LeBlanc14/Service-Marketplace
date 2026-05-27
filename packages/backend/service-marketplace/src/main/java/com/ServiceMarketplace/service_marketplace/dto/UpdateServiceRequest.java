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
public class UpdateServiceRequest {

    public static final int MAX_TITLE_LENGTH = 80;
    public static final int MAX_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_TAG_COUNT = 5;
    public static final int MAX_TAG_LENGTH = 50;

    @NotBlank
    @Size(max = MAX_TITLE_LENGTH, message = "Title must be 80 characters or fewer")
    private String title;

    @NotBlank
    @Size(max = MAX_DESCRIPTION_LENGTH, message = "Description must be 1000 characters or fewer")
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

    @Size(max = MAX_TAG_COUNT, message = "Use no more than 5 tags")
    private List<@Size(max = MAX_TAG_LENGTH, message = "Tags must be 50 characters or fewer") String> tags;

    @AssertTrue(message = "Price max must be greater than or equal to price min")
    public boolean isPriceRangeValid() {
        return priceMin == null || priceMax == null || priceMax.compareTo(priceMin) >= 0;
    }
}
