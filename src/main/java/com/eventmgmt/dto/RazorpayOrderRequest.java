package com.eventmgmt.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class RazorpayOrderRequest {

    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotNull(message = "Ticket count is required")
    @Min(value = 1, message = "Must purchase at least 1 ticket")
    private Integer ticketCount;

    public RazorpayOrderRequest() {
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Integer getTicketCount() {
        return ticketCount;
    }

    public void setTicketCount(Integer ticketCount) {
        this.ticketCount = ticketCount;
    }
}
