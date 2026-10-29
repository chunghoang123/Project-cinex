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
        Booking booking = bookingService.createHold(user.getId(), request.getShowtimeId(),
            request.getSeatIds(), request.getSnackAmount(), request.getSnackNote());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Giữ ghế thành công", bookingMapper.toResponse(booking)));
    }

    /**
     * Tạo link thanh toán PayOS cho đơn đặt vé online.
     */
    @PostMapping("/{bookingCode}/payos")
    public ResponseEntity<ApiResponse<PayosPaymentResponse>> createPayosPayment(
            @PathVariable String bookingCode,
            Authentication authentication) {
        Booking booking = bookingService.findByBookingCode(bookingCode);
        checkAccess(booking, currentUser(authentication));
        PayosPaymentResponse payment = bookingService.createPayosPayment(bookingCode);
        return ResponseEntity.ok(ApiResponse.ok("Tạo mã thanh toán PayOS thành công", payment));
    }

    /**
     * Đồng bộ trạng thái thanh toán PayOS cho đơn hàng (dự phòng khi webhook chưa tới).
     */
    @PostMapping("/{bookingCode}/payos/sync")
    public ResponseEntity<ApiResponse<BookingResponse>> syncPayosPayment(
            @PathVariable String bookingCode,
            Authentication authentication) {
        Booking booking = bookingService.findByBookingCode(bookingCode);
        checkAccess(booking, currentUser(authentication));
        Booking synced = bookingService.syncPayosPayment(bookingCode);
        return ResponseEntity.ok(ApiResponse.ok(bookingMapper.toResponse(synced)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> myBookings(Authentication authentication) {
        User user = currentUser(authentication);
        List<BookingResponse> bookings = bookingService.findMyBookings(user.getId()).stream()
            .map(bookingMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(bookings));
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
