package com.eventmgmt.controller;

import com.eventmgmt.dto.BookingRequest;
import com.eventmgmt.dto.PaymentVerifyRequest;
import com.eventmgmt.dto.RazorpayOrderRequest;
import com.eventmgmt.model.Booking;
import com.eventmgmt.model.User;
import com.eventmgmt.service.BookingService;
import com.eventmgmt.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final UserService userService;

    public BookingController(BookingService bookingService, UserService userService) {
        this.bookingService = bookingService;
        this.userService = userService;
    }

    /**
     * Create a Razorpay Order.
     */
    @PostMapping("/create-order")
    public ResponseEntity<?> createRazorpayOrder(@Valid @RequestBody RazorpayOrderRequest request) {
        try {
            User currentUser = userService.getCurrentUser();
            String orderId = bookingService.createRazorpayOrder(
                    request.getEventId(), request.getTicketCount(), currentUser
            );
            
            Map<String, String> response = new HashMap<>();
            response.put("orderId", orderId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Verify Payment and Confirm Booking.
     */
    @PostMapping("/verify-payment")
    public ResponseEntity<Booking> verifyPayment(@Valid @RequestBody PaymentVerifyRequest request, @RequestParam Long eventId, @RequestParam Integer ticketCount) {
        User currentUser = userService.getCurrentUser();
        Booking booking = bookingService.confirmBooking(
                eventId, ticketCount,
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature(),
                currentUser
        );
        return new ResponseEntity<>(booking, HttpStatus.CREATED);
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<List<Booking>> getMyBookings() {
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(bookingService.getMyBookings(currentUser));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<Booking>> getEventBookings(@PathVariable Long eventId) {
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(bookingService.getEventBookings(eventId, currentUser));
    }
}
