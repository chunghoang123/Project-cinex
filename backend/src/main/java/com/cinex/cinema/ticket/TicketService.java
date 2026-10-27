package com.cinex.cinema.ticket;

import com.cinex.cinema.booking.Booking;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public Ticket issue(Booking booking) {
        return ticketRepository.findByBookingId(booking.getId()).orElseGet(() -> {
            String code = "TK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Ticket ticket = Ticket.builder()
                .booking(booking)
                .ticketCode(code)
                .qrCode(code)
                .status(TicketStatus.UNUSED)
                .build();
            return ticketRepository.save(ticket);
        });
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public Ticket validate(String code) {
        String normalized = code == null ? "" : code.trim();
        Ticket ticket = ticketRepository.findByTicketCode(normalized)
            .or(() -> ticketRepository.findByQrCode(normalized))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vé không tồn tại"));

        if (ticket.getStatus() == TicketStatus.SCANNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Vé đã được soát lúc " + ticket.getScannedAt());
        }
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vé đã bị hủy");
        }

        ticket.setStatus(TicketStatus.SCANNED);
        ticket.setScannedAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    public void cancel(Booking booking) {
        ticketRepository.findByBookingId(booking.getId()).ifPresent(ticket -> {
            ticket.setStatus(TicketStatus.CANCELLED);
            ticketRepository.save(ticket);
        });
    }

    public Ticket findByCode(String code) {
        String normalized = code == null ? "" : code.trim();
        return ticketRepository.findByTicketCode(normalized)
            .or(() -> ticketRepository.findByQrCode(normalized))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vé không tồn tại"));
    }
}
