package com.ServiceMarketplace.service_marketplace.dto;

import lombok.Value;

@Value
public class ConnectStatusResponse {
    String accountId;
    boolean chargesEnabled;
    boolean detailsSubmitted;
    boolean payoutsEnabled;
}
