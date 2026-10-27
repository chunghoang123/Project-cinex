package com.cinex.cinema.booking;

import com.cinex.cinema.common.BaseEntity;
import com.cinex.cinema.showtime.Showtime;
import com.cinex.cinema.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings", uniqueConstraints = @UniqueConstraint(columnNames = "booking_code"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseEntity {
    @Column(name = "booking_code", nullable = false, length = 20, unique = true)
    private String bookingCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "seat_amount", precision = 12, scale = 2)
    private BigDecimal seatAmount;

    @Column(name = "snack_amount", precision = 12, scale = 2)
    private BigDecimal snackAmount;

    @Column(name = "snack_note", length = 500)
    private String snackNote;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private BookingChannel channel = BookingChannel.ONLINE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private User staff;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}