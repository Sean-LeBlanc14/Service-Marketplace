package com.ServiceMarketplace.service_marketplace.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.ServiceMarketplace.service_marketplace.model.SupportEntry;
import com.ServiceMarketplace.service_marketplace.model.SupportEntryType;

@Repository
public interface SupportEntryRepository extends MongoRepository<SupportEntry, String> {
    
    Optional<List<SupportEntry>> findBySupportEntryTypeOrderByCreatedAtDesc(SupportEntryType supportEntryType);

    Optional<SupportEntry> findByReporterEmail(String email);


}
