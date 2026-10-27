package com.cinex.cinema.staff;

import com.cinex.cinema.booking.BookingResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketValidationResponse {
    private boolean valid;
    private String message;
    private String ticketCode;
    private String ticketStatus;
    private LocalDateTime scannedAt;
    private BookingResponse booking;
}
