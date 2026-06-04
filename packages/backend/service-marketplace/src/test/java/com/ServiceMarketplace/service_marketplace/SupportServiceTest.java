package com.ServiceMarketplace.service_marketplace;

import com.ServiceMarketplace.service_marketplace.dto.SupportRequest;
import com.ServiceMarketplace.service_marketplace.dto.SupportResponse;
import com.ServiceMarketplace.service_marketplace.model.SupportEntry;
import com.ServiceMarketplace.service_marketplace.model.SupportEntryType;
import com.ServiceMarketplace.service_marketplace.repository.SupportEntryRepository;
import com.ServiceMarketplace.service_marketplace.service.SupportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SupportServiceTest {

    @Mock
    private SupportEntryRepository supportEntryRepository;

    @InjectMocks
    private SupportService supportService;

    private UserDetails mockUserDetails() {
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("student@example.com");
        return userDetails;
    }

    @Test
    void reportBug_savesBugSupportEntryAndReturnsResponse() {
        UserDetails userDetails = mockUserDetails();
        SupportRequest request = new SupportRequest("The booking calendar fails to load.");
        Instant createdAt = Instant.parse("2026-06-02T18:30:00Z");

        when(supportEntryRepository.save(any(SupportEntry.class)))
                .thenAnswer(invocation -> {
                    SupportEntry entry = invocation.getArgument(0);
                    entry.setId("support123");
                    entry.setCreatedAt(createdAt);
                    return entry;
                });

        SupportResponse response = supportService.reportBug(userDetails, request);

        ArgumentCaptor<SupportEntry> savedEntry = ArgumentCaptor.forClass(SupportEntry.class);
        verify(supportEntryRepository).save(savedEntry.capture());

        assertEquals("student@example.com", savedEntry.getValue().getReporterEmail());
        assertEquals("The booking calendar fails to load.", savedEntry.getValue().getContext());
        assertEquals(SupportEntryType.BUG, savedEntry.getValue().getSupportEntryType());
        assertEquals(createdAt, response.getReportDate());
        assertEquals(
                "This bug has been successfully reported. Thank you for helping maintain PolyServices seamless.",
                response.getMessage()
        );
    }

    @Test
    void contactSupport_savesInquirySupportEntryAndReturnsResponse() {
        UserDetails userDetails = mockUserDetails();
        SupportRequest request = new SupportRequest("I need help changing my payout account.");
        Instant createdAt = Instant.parse("2026-06-02T19:15:00Z");

        when(supportEntryRepository.save(any(SupportEntry.class)))
                .thenAnswer(invocation -> {
                    SupportEntry entry = invocation.getArgument(0);
                    entry.setId("support456");
                    entry.setCreatedAt(createdAt);
                    return entry;
                });

        SupportResponse response = supportService.contactSupport(userDetails, request);

        ArgumentCaptor<SupportEntry> savedEntry = ArgumentCaptor.forClass(SupportEntry.class);
        verify(supportEntryRepository).save(savedEntry.capture());

        assertEquals("student@example.com", savedEntry.getValue().getReporterEmail());
        assertEquals("I need help changing my payout account.", savedEntry.getValue().getContext());
        assertEquals(SupportEntryType.INQUIRY, savedEntry.getValue().getSupportEntryType());
        assertEquals(createdAt, response.getReportDate());
        assertEquals(
                "You have successfully contacted our support team, you will get a response shortly",
                response.getMessage()
        );
    }
}
