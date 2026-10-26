package com.cinex.cinema.ticket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByTicketCode(String ticketCode);
    Optional<Ticket> findByQrCode(String qrCode);
    Optional<Ticket> findByBookingId(Long bookingId);
    List<Ticket> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}