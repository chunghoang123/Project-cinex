package com.cinex.cinema.staff;

import com.cinex.cinema.booking.BookingResponse;
import com.cinex.cinema.common.ApiResponse;
import com.cinex.cinema.showtime.ShowtimeResponse;
import com.cinex.cinema.showtime.ShowtimeSeatResponse;
import com.cinex.cinema.user.User;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/staff")
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping("/showtimes")
    public ResponseEntity<ApiResponse<List<ShowtimeResponse>>> showtimes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long cinemaId) {
        return ResponseEntity.ok(ApiResponse.ok(staffService.listShowtimes(date, cinemaId)));
    }

    @GetMapping("/showtimes/{id}/seats")
    public ResponseEntity<ApiResponse<List<ShowtimeSeatResponse>>> seats(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(staffService.getSeats(id)));
    }

    @PostMapping("/sales")
    public ResponseEntity<ApiResponse<StaffSaleResponse>> createSale(
            @Valid @RequestBody StaffSaleRequest request, Authentication authentication) {
        User staff = (User) authentication.getPrincipal();
        StaffSaleResponse sale = staffService.createSale(staff.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Bán vé thành công", sale));
    }

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> bookings(
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.ok(staffService.search(q)));
    }

    @GetMapping("/bookings/{code}")
    public ResponseEntity<ApiResponse<BookingResponse>> booking(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok(staffService.getBooking(code)));
    }

    @PostMapping("/bookings/{code}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancel(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok("Hủy đơn thành công", staffService.cancel(code)));
    }

    @PostMapping("/tickets/validate")
    public ResponseEntity<ApiResponse<TicketValidationResponse>> validateTicket(
            @Valid @RequestBody TicketValidationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(staffService.validateTicket(request.getCode())));
    }

    @GetMapping("/shift-summary")
    public ResponseEntity<ApiResponse<ShiftSummaryResponse>> shiftSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(staffService.shiftSummary(date)));
    }
}
