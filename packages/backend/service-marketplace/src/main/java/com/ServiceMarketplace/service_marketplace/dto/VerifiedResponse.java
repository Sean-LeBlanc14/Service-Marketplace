package com.ServiceMarketplace.service_marketplace.dto;

import java.time.LocalDateTime;

public record VerifiedResponse(String message, boolean verified, LocalDateTime time) {
    
}
