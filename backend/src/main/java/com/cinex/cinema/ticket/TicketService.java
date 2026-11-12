package com.cinex.cinema.ticket;

import com.cinex.cinema.booking.Booking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketValidator ticketValidator;

    @Autowired
    public TicketService(TicketRepository ticketRepository, TicketValidator ticketValidator) {
        this.ticketRepository = ticketRepository;
        this.ticketValidator = ticketValidator;
    }

    public TicketService(TicketRepository ticketRepository) {
        this(ticketRepository, new TicketValidator(ticketRepository));
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

    public Ticket validate(String ticketCode) {
        return ticketValidator.validateTicket(ticketCode);
    }

    public Ticket validateTicket(String ticketCode) {
        return ticketValidator.validateTicket(ticketCode);
    }

    public void cancel(Booking booking) {
        Ticket ticket = ticketRepository.findByBookingId(booking.getId()).orElse(null);
        if (ticket != null) {
            ticket.setStatus(TicketStatus.CANCELLED);
            ticketRepository.save(ticket);
        }
    }
}
