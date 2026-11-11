package com.cinex.cinema.ticket;

import com.cinex.cinema.common.ApiResponse;
import com.cinex.cinema.user.ERole;
import com.cinex.cinema.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/{ticketCode}")
    public ResponseEntity<ApiResponse<Ticket>> getByCode(@PathVariable String ticketCode,
                                                         Authentication authentication) {
        Ticket ticket = ticketService.findByTicketCode(ticketCode);
        return ResponseEntity.ok(ApiResponse.ok(ticket));
    }

    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<Ticket>> validate(@RequestBody java.util.Map<String, String> request) {
        String ticketCode = request.get("ticketCode");
        Ticket ticket = ticketService.validateTicket(ticketCode);
        return ResponseEntity.ok(ApiResponse.ok("Vé hợp lệ", ticket));
    }
}