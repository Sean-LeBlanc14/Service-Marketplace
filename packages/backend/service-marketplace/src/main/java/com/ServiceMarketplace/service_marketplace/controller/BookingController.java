package com.ServiceMarketplace.service_marketplace.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ServiceMarketplace.service_marketplace.dto.BookingResponse;
import com.ServiceMarketplace.service_marketplace.dto.CreateBookingRequest;
import com.ServiceMarketplace.service_marketplace.dto.CreateBookingResponse;
import com.ServiceMarketplace.service_marketplace.service.BookingService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<CreateBookingResponse> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        CreateBookingResponse response = bookingService.createBooking(request, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<BookingResponse>> getBooking(@AuthenticationPrincipal UserDetails userDetails){

        List<BookingResponse> response = bookingService.getUserBookings(userDetails);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/completed")
    public ResponseEntity<List<BookingResponse>> getCompletedBookings(@AuthenticationPrincipal UserDetails userDetails) {
        
        List<BookingResponse> response = bookingService.getUserCompletedBookings(userDetails);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
}