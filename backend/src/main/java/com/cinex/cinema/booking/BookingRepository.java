package com.cinex.cinema.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingCode(String bookingCode);
    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime time);
    List<Booking> findAllByOrderByCreatedAtDesc();
    List<Booking> findTop8ByOrderByCreatedAtDesc();
    List<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status);
    List<Booking> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Booking> findByUserUsernameOrderByCreatedAtDesc(String username);
    List<Booking> findByBookingCodeContainingIgnoreCaseOrCustomerPhoneContainingIgnoreCaseOrCustomerNameContainingIgnoreCase(
        String bookingCode, String customerPhone, String customerName);
    boolean existsByShowtimeId(Long showtimeId);
    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
