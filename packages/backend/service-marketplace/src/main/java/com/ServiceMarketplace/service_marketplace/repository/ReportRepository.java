package com.ServiceMarketplace.service_marketplace.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.ServiceMarketplace.service_marketplace.model.Report;

@Repository
public interface ReportRepository extends MongoRepository<Report, String> {
    List<Report> findByStatus(String status);
}
