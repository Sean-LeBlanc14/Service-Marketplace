package com.ServiceMarketplace.service_marketplace.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ServiceMarketplace.service_marketplace.model.Report;
import com.ServiceMarketplace.service_marketplace.model.User;
import com.ServiceMarketplace.service_marketplace.repository.ReportRepository;
import com.ServiceMarketplace.service_marketplace.service.UserService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportRepository reportRepository;
    private final UserService userService;

    public ReportController(ReportRepository reportRepository, UserService userService) {
        this.reportRepository = reportRepository;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<Report> createReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CreateReportRequest request) {
        User reporter = userService.getUserByEmail(userDetails.getUsername());

        Report report = new Report();
        report.setReporterId(reporter.getId());
        report.setListingId(request.listingId());
        report.setProviderId(request.providerId());
        report.setReason(request.reason());

        return ResponseEntity.status(HttpStatus.CREATED).body(reportRepository.save(report));
    }

    @GetMapping
    public ResponseEntity<List<Report>> getReports(@AuthenticationPrincipal UserDetails userDetails) {
        if (!userService.isAdmin(userDetails.getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.status(HttpStatus.OK).body(reportRepository.findAll());
    }

    @PutMapping("/{reportId}/resolve")
    public ResponseEntity<Report> resolveReport(
            @PathVariable String reportId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (!userService.isAdmin(userDetails.getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        var reportToResolve = reportRepository.findById(reportId);

        if (reportToResolve.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Report report = reportToResolve.get();
        report.setStatus("resolved");

        return ResponseEntity.status(HttpStatus.OK).body(reportRepository.save(report));
    }

    public static record CreateReportRequest(String listingId, String providerId, String reason) {
    }
}
