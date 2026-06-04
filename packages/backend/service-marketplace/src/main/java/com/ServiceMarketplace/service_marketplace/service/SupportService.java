package com.ServiceMarketplace.service_marketplace.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.ServiceMarketplace.service_marketplace.dto.SupportRequest;
import com.ServiceMarketplace.service_marketplace.dto.SupportResponse;
import com.ServiceMarketplace.service_marketplace.model.SupportEntry;
import com.ServiceMarketplace.service_marketplace.model.SupportEntryType;
import com.ServiceMarketplace.service_marketplace.repository.SupportEntryRepository;

@Service
public class SupportService {

    private final SupportEntryRepository supportEntryRepository;

    public SupportService(SupportEntryRepository supportEntryRepository) {
        this.supportEntryRepository = supportEntryRepository;
    }

    public SupportResponse reportBug(UserDetails userDetails, SupportRequest request){

        SupportEntry entry = new SupportEntry();

        entry.setContext(request.getContext());
        entry.setReporterEmail(userDetails.getUsername());
        entry.setSupportEntryType(SupportEntryType.BUG);

        supportEntryRepository.save(entry);

        String bugReportMessage = "This bug has been successfully reported. Thank you for helping maintain PolyServices seamless.";

        return toSupportResponse(entry, bugReportMessage);

    }

    public SupportResponse contactSupport(UserDetails userDetails, SupportRequest request){
        SupportEntry entry = new SupportEntry();

        entry.setContext(request.getContext());
        entry.setReporterEmail(userDetails.getUsername());
        entry.setSupportEntryType(SupportEntryType.INQUIRY);

        supportEntryRepository.save(entry);

        String message = "You have successfully contacted our support team, you will get a response shortly";

        return toSupportResponse(entry, message);
    }

    private SupportResponse toSupportResponse(SupportEntry entry, String message){
        return new SupportResponse(entry.getCreatedAt(), message);
    }
    
}
