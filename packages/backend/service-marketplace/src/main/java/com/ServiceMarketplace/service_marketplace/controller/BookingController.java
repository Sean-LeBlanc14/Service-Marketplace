package com.ServiceMarketplace.service_marketplace.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
import com.ServiceMarketplace.service_marketplace.exception.BookingStateException;
import com.ServiceMarketplace.service_marketplace.exception.BookingTokenException;
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

    @GetMapping("/action")
    public ResponseEntity<String> handleBookingAction(@RequestParam String token) {
        try {
            BookingTokenAction action = bookingService.processTokenAction(token);
            String title = action == BookingTokenAction.CONFIRM ? "Booking Confirmed" : "Booking Cancelled";
            String message = action == BookingTokenAction.CONFIRM
                ? "The booking has been confirmed and the customer's payment is being processed."
                : "The booking request has been cancelled and the customer's card details have been removed.";
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(buildHtmlPage(title, message, "#2e7d32"));
        } catch (BookingTokenException e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.TEXT_HTML)
                .body(buildHtmlPage("Link Invalid", e.getMessage(), "#c62828"));
        } catch (BookingStateException e) {
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(buildHtmlPage("Already Actioned", "This booking has already been confirmed or cancelled.", "#e65100"));
        }
    }

    @GetMapping("/requests")
    public ResponseEntity<List<BookingResponse>> getRequests(@AuthenticationPrincipal UserDetails userDetails) {
      List<BookingResponse> response = bookingService.getUserBookingRequests(userDetails);

      return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/completed")
    public ResponseEntity<List<BookingResponse>> getCompletedBookings(@AuthenticationPrincipal UserDetails userDetails){
      List<BookingResponse> response = bookingService.getUserCompletedBookings(userDetails);

      return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/scheduled")
    public ResponseEntity<List<BookingResponse>> getScheduledBookings(@AuthenticationPrincipal UserDetails userDetails) {
      List<BookingResponse> response = bookingService.getUserScheduledBookings(userDetails);

      return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    

    private String buildHtmlPage(String heading, String message, String headingColor) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>%s</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                <tr>
                  <td align="center" style="padding:80px 20px;">
                    <table width="100%%" cellpadding="0" cellspacing="0" border="0"
                           style="max-width:500px;background-color:#ffffff;border-radius:8px;padding:40px;">
                      <tr>
                        <td align="center">
                          <h1 style="margin:0;color:%s;">%s</h1>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding-top:20px;color:#555555;font-size:16px;line-height:24px;text-align:center;">
                          %s
                        </td>
                      </tr>
                      <tr>
                        <td align="center" style="padding-top:30px;font-size:12px;color:#999999;">
                          &copy; 2026 Service Market Place. All rights reserved.
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(heading, headingColor, heading, message);
    }
}
