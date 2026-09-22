package com.eventmgmt.service;

import com.eventmgmt.exception.ResourceNotFoundException;
import com.eventmgmt.exception.TicketCapacityExceededException;
import com.eventmgmt.exception.UnauthorizedAccessException;
import com.eventmgmt.model.Booking;
import com.eventmgmt.model.Event;
import com.eventmgmt.model.User;
import com.eventmgmt.repository.BookingRepository;
import com.eventmgmt.repository.EventRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final PaymentService paymentService;
    private final EmailService emailService;

    public BookingService(BookingRepository bookingRepository, EventRepository eventRepository,
                          PaymentService paymentService, EmailService emailService) {
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
        this.paymentService = paymentService;
        this.emailService = emailService;
    }

    /**
     * Step 1: Create a Razorpay order and reserve tickets.
     * Returns the Razorpay Order ID.
     */
    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public String createRazorpayOrder(Long eventId, int ticketCount, User attendee) throws Exception {
        if (ticketCount <= 0) {
            throw new IllegalArgumentException("Ticket count must be greater than zero.");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        if (event.getDateTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot book tickets for past events.");
        }

        if (event.getTicketsRemaining() < ticketCount) {
            throw new TicketCapacityExceededException(
                    "Only " + event.getTicketsRemaining() + " tickets are available for this event.");
        }

        double totalAmount = event.getPrice() * ticketCount;
        String receiptId = "RCPT_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        // Create order in Razorpay
        return paymentService.createRazorpayOrder(totalAmount, receiptId);
    }

    /**
     * Step 2: Verify payment and confirm booking.
     */
    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public Booking confirmBooking(Long eventId, int ticketCount, String razorpayOrderId, 
                                  String razorpayPaymentId, String razorpaySignature, User attendee) {
        
        // 1. Verify Payment Signature
        paymentService.verifyPaymentSignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        // 2. Check Ticket Availability again (double check)
        if (event.getTicketsRemaining() < ticketCount) {
            throw new TicketCapacityExceededException("Tickets no longer available. Please retry.");
        }

        double totalAmount = event.getPrice() * ticketCount;

        // 3. Save Transaction
        paymentService.saveSuccessfulTransaction(totalAmount, razorpayOrderId, razorpayPaymentId, attendee.getFullName());

        // 4. Deduct Tickets
        event.setTicketsSold(event.getTicketsSold() + ticketCount);
        eventRepository.save(event);

        // 5. Create Booking Record
        Booking booking = new Booking(
                attendee,
                event,
                LocalDateTime.now(),
                ticketCount,
                totalAmount,
                "CONFIRMED",
                razorpayPaymentId
        );

        Booking savedBooking = bookingRepository.save(booking);

        // 6. Send Email
        emailService.sendTicketConfirmation(
                attendee.getEmail(),
                attendee.getFullName(),
                event.getTitle(),
                ticketCount,
                totalAmount,
                razorpayPaymentId
        );

        return savedBooking;
    }

    public List<Booking> getMyBookings(User attendee) {
        return bookingRepository.findByAttendeeIdOrderByBookingDateDesc(attendee.getId());
    }

    public List<Booking> getEventBookings(Long eventId, User organizer) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        if (!event.getOrganizer().getId().equals(organizer.getId()) && 
            !organizer.getRole().equals("ROLE_ADMIN")) {
            throw new UnauthorizedAccessException("You are not authorized to view bookings for this event.");
        }

        return bookingRepository.findByEventId(eventId);
    }
}
