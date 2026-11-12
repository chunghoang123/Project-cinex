package com.cinex.cinema.ticket;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * Bean độc lập xử lý soát vé trong transaction mới (REQUIRES_NEW) thông qua Spring AOP proxy,
 * đảm bảo khi vé không hợp lệ và ném ngoại lệ thì transaction của caller (vd StaffService)
 * không bị đánh dấu rollback-only (tránh lỗi Sonar S2229 self-invocation).
 */
@Component
public class TicketValidator {

    private final TicketRepository ticketRepository;

    public TicketValidator(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Ticket validateTicket(String ticketCode) {
        Ticket ticket = findByTicketCode(ticketCode);

        if (ticket.getStatus() == TicketStatus.SCANNED) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Vé đã được soát, không thể dùng lại");
        }

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Vé đã bị hủy");
        }

        ticket.setStatus(TicketStatus.SCANNED);
        ticket.setScannedAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    @Transactional(readOnly = true)
    public Ticket findByTicketCode(String ticketCode) {
        return ticketRepository.findByTicketCode(normalizeCode(ticketCode))
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Vé không tồn tại"));
    }

    /** Chấp nhận cả mã vé và nội dung QR đầy đủ dạng CINEX|TKT-...|BK... khi quét. */
    public String normalizeCode(String ticketCode) {
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
}
