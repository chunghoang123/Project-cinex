package com.cinex.cinema.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private Long id;
    private String bookingCode;
    private String customerName;
    private String customerPhone;
    private String channel;
    private String status;
    private BigDecimal seatAmount;
    private BigDecimal snackAmount;
    private String snackNote;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String paymentStatus;
    private String ticketCode;
    private String ticketStatus;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime paidAt;
    private BookingShowtimeResponse showtime;
    private List<BookingSeatResponse> seats;
}
