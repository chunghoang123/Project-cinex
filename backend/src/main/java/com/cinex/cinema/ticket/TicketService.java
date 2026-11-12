package com.cinex.cinema.ticket;

import com.cinex.cinema.booking.Booking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public Ticket issue(Booking booking) {
        String ticketCode = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String qrContent = "CINEX|" + ticketCode + "|" + booking.getBookingCode();

        Ticket ticket = Ticket.builder()
            .booking(booking)
            .ticketCode(ticketCode)
            .qrCode(qrContent)
            .status(TicketStatus.UNUSED)
            .build();

        return ticketRepository.save(ticket);
    }

    @Transactional(readOnly = true)
    public Ticket findByTicketCode(String ticketCode) {
        return ticketValidator.findByTicketCode(ticketCode);
    }

    /** Chấp nhận cả mã vé và nội dung QR đầy đủ dạng CINEX|TKT-...|BK... khi quét. */
    private String normalizeCode(String ticketCode) {
        if (ticketCode == null) {
            return null;
        }
        String value = ticketCode.trim();
        if (value.startsWith("CINEX|")) {
            String[] parts = value.split("\\|");
            if (parts.length >= 2 && !parts[1].isBlank()) {
                return parts[1].trim();
            }
        }
        return value;
    }

    public Ticket validate(String ticketCode) {
        return validateTicket(ticketCode);
    }

    public void cancel(Booking booking) {
        Ticket ticket = ticketRepository.findByBookingId(booking.getId()).orElse(null);
        if (ticket != null) {
            ticket.setStatus(TicketStatus.CANCELLED);
            ticketRepository.save(ticket);
        }
    }
}
