package com.ServiceMarketplace.service_marketplace.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ServiceMarketplace.service_marketplace.dto.BookingResponse;
import com.ServiceMarketplace.service_marketplace.dto.ConfirmBookingRequest;
import com.ServiceMarketplace.service_marketplace.dto.CreateBookingRequest;
import com.ServiceMarketplace.service_marketplace.dto.CreateBookingResponse;
import com.ServiceMarketplace.service_marketplace.dto.ProviderReviewResponse;
import com.ServiceMarketplace.service_marketplace.dto.SubmitReviewRequest;
import com.ServiceMarketplace.service_marketplace.exception.BookingStateException;
import com.ServiceMarketplace.service_marketplace.exception.BookingTokenException;
import com.ServiceMarketplace.service_marketplace.exception.ResourceNotFoundException;
import com.ServiceMarketplace.service_marketplace.model.BookingTokenAction;
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
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/customer/me")
    public ResponseEntity<List<BookingResponse>> getCustomerBookings(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<BookingResponse> response = bookingService.getCustomerBookings(userDetails);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/customer/scheduled/me")
    public ResponseEntity<List<BookingResponse>> getCustomerScheduledBookings(@AuthenticationPrincipal UserDetails userDetails){

      List<BookingResponse> response = bookingService.getCustomerScheduledBookings(userDetails);

      return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @GetMapping("/provider/{providerId}/reviews")
    public ResponseEntity<List<ProviderReviewResponse>> getProviderReviews(
            @PathVariable String providerId) {
        List<ProviderReviewResponse> response = bookingService.getProviderReviews(providerId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/provider/requests/me")
    public ResponseEntity<List<BookingResponse>> getProviderBookingRequests(@AuthenticationPrincipal UserDetails userDetails){
        List<BookingResponse> response = bookingService.getProviderBookingRequests(userDetails);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/provider/scheduled/me")
    public ResponseEntity<List<BookingResponse>> getProviderScheduledBookings(@AuthenticationPrincipal UserDetails userDetails){
        List<BookingResponse> response = bookingService.getProviderScheduledBookings(userDetails);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/provider/completed/me")
    public ResponseEntity<List<BookingResponse>> getProviderCompletedBookings(@AuthenticationPrincipal UserDetails userDetails){
        List<BookingResponse> response = bookingService.getProviderCompletedBookings(userDetails);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{bookingId}/review")
    public ResponseEntity<BookingResponse> submitReview(
            @PathVariable String bookingId,
            @Valid @RequestBody SubmitReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        BookingResponse response = bookingService.submitReview(bookingId, request, userDetails);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(
            @PathVariable String id,
            @Valid @RequestBody ConfirmBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        BookingResponse response = bookingService.confirmBooking(id, request, userDetails);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        BookingResponse response = bookingService.cancelBooking(id, userDetails);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<BookingResponse> completeBooking(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        BookingResponse response = bookingService.completeBooking(id, userDetails);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/{id}/reject")
    public ResponseEntity<BookingResponse> rejectBooking(@PathVariable String id, @AuthenticationPrincipal UserDetails userDetails){
        
        BookingResponse response = bookingService.rejectBooking(id, userDetails);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/action")
    public ResponseEntity<Map<String, String>> handleBookingAction(@RequestParam String token) {
        try {
            BookingTokenAction action = bookingService.processTokenAction(token);
            String title = action == BookingTokenAction.CONFIRM ? "Booking Confirmed" : "Booking Cancelled";
            String message = action == BookingTokenAction.CONFIRM
                    ? "The booking has been confirmed and the customer's payment is being processed."
                    : "The booking request has been cancelled and the customer's card details have been removed.";
            return ResponseEntity.ok(Map.of("status", "success", "title", title, "message", message));
        } catch (BookingTokenException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "title", "Link Invalid", "message", e.getMessage()));
        } catch (BookingStateException e) {
            return ResponseEntity.ok(Map.of("status", "warning", "title", "Already Actioned",
                    "message", "This booking has already been confirmed or cancelled."));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "title", "Link Invalid", "message", "This booking no longer exists."));
        }
    }
}
