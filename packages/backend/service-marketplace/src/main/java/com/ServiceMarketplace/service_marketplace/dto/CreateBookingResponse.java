package com.ServiceMarketplace.service_marketplace.dto;

import lombok.Value;

@Value
public class CreateBookingResponse {
    private BookingResponse booking;
    private String clientSecret;
}