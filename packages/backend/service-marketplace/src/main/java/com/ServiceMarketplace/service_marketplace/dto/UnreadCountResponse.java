package com.ServiceMarketplace.service_marketplace.dto;

import lombok.Value;

@Value
public class UnreadCountResponse {
    long messages;
    long notifications;
}
