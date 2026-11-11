package com.cinex.cinema.admin;

import com.cinex.cinema.booking.BookingResponse;
import com.cinex.cinema.booking.BookingStatus;
import com.cinex.cinema.common.ApiResponse;
import com.cinex.cinema.user.CreateUserRequest;
import com.cinex.cinema.user.UpdateUserRolesRequest;
import com.cinex.cinema.user.UpdateUserStatusRequest;
import com.cinex.cinema.user.UserResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOperationsController {

    private final AdminDashboardService dashboardService;
    private final AdminBookingService bookingService;
    private final AdminUserService userService;

    public AdminOperationsController(AdminDashboardService dashboardService,
                                     AdminBookingService bookingService) {
        this.dashboardService = dashboardService;
        this.bookingService = bookingService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> dashboard() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDashboard()));
    }

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> bookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.list(status, date, q)));
    }

    @PostMapping("/bookings/{code}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok("Hủy đơn thành công", bookingService.cancel(code)));
    }
}
