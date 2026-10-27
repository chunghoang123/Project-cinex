package com.cinex.cinema.booking;

import com.cinex.cinema.common.ApiResponse;
import com.cinex.cinema.user.ERole;
import com.cinex.cinema.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final BookingMapper bookingMapper;

    public BookingController(BookingService bookingService, BookingMapper bookingMapper) {
        this.bookingService = bookingService;
        this.bookingMapper = bookingMapper;
    }

    @PostMapping("/holds")
    public ResponseEntity<ApiResponse<BookingResponse>> createHold(
            @Valid @RequestBody HoldRequest request,
            Authentication authentication) {
        User user = currentUser(authentication);
        Booking booking = bookingService.createHold(user.getId(), request.getShowtimeId(), request.getSeatIds());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Giữ ghế thành công", bookingMapper.toResponse(booking)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> myBookings(Authentication authentication) {
        User user = currentUser(authentication);
        List<BookingResponse> bookings = bookingService.findMyBookings(user.getId()).stream()
            .map(bookingMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(bookings));
    }

    @PostMapping("/{bookingCode}/confirm")
    public ResponseEntity<ApiResponse<BookingResponse>> confirm(@PathVariable String bookingCode,
                                                                Authentication authentication) {
        Booking booking = bookingService.findByBookingCode(bookingCode);
        checkAccess(booking, currentUser(authentication));
        Booking confirmed = bookingService.confirmBooking(bookingCode);
        return ResponseEntity.ok(ApiResponse.ok("Xác nhận đơn thành công", bookingMapper.toResponse(confirmed)));
    }

    @PostMapping("/{bookingCode}/pay")
    public ResponseEntity<ApiResponse<BookingResponse>> pay(@PathVariable String bookingCode,
                                                            @RequestBody(required = false) PaymentRequest request,
                                                            Authentication authentication) {
        Booking booking = bookingService.findByBookingCode(bookingCode);
        checkAccess(booking, currentUser(authentication));
        String method = request == null || request.getPaymentMethod() == null
            ? "ONLINE" : request.getPaymentMethod();
        Booking paid = bookingService.payBooking(bookingCode, method);
        return ResponseEntity.ok(ApiResponse.ok("Thanh toán thành công", bookingMapper.toResponse(paid)));
    }

    @PostMapping("/{bookingCode}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancel(@PathVariable String bookingCode,
                                                               Authentication authentication) {
        Booking booking = bookingService.findByBookingCode(bookingCode);
        checkAccess(booking, currentUser(authentication));
        Booking cancelled = bookingService.cancelBooking(bookingCode);
        return ResponseEntity.ok(ApiResponse.ok("Hủy đơn thành công", bookingMapper.toResponse(cancelled)));
    }

    @GetMapping("/{bookingCode}")
    public ResponseEntity<ApiResponse<BookingResponse>> getByCode(@PathVariable String bookingCode,
                                                                  Authentication authentication) {
        Booking booking = bookingService.findByBookingCode(bookingCode);
        checkAccess(booking, currentUser(authentication));
        return ResponseEntity.ok(ApiResponse.ok(bookingMapper.toResponse(booking)));
    }

    private User currentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

    private void checkAccess(Booking booking, User user) {
        if (isStaffOrAdmin(user) || bookingService.isOwner(booking, user.getId())) {
            return;
        }
        throw new org.springframework.web.server.ResponseStatusException(
            HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập đơn này");
    }

    private boolean isStaffOrAdmin(User user) {
        return user.getRoles().stream()
            .anyMatch(role -> role.getName() == ERole.ROLE_ADMIN || role.getName() == ERole.ROLE_STAFF);
    }
}
