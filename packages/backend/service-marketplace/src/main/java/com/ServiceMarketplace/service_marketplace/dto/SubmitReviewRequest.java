package com.ServiceMarketplace.service_marketplace.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubmitReviewRequest {

    public static final int MAX_REVIEW_LENGTH = 1000;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @NotBlank
    @Size(max = MAX_REVIEW_LENGTH, message = "Review must be 1000 characters or fewer")
    private String review;
}
