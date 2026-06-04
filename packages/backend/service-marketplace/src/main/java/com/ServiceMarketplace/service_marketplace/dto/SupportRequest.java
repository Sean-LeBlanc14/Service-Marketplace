package com.ServiceMarketplace.service_marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class SupportRequest {
    
    @NotBlank
    @Size(min= 10, max = 1000)
    String context;

}
