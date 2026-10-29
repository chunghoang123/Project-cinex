package com.cinex.cinema.payment;

import com.cinex.cinema.booking.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final BookingService bookingService;

    public PaymentController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Webhook PayOS: PayOS gọi endpoint này khi trạng thái thanh toán thay đổi.
     * Endpoint này không yêu cầu xác thực (PayOS gọi server-to-server).
     */
    @PostMapping("/payos/webhook")
    public ResponseEntity<Map<String, Object>> payosWebhook(@RequestBody Map<String, Object> body) {
        try {
            bookingService.handlePayOSWebhook(body);
        } catch (Exception e) {
            // PayOS yêu cầu trả 200 để không retry; ghi nhận lỗi trong payload
            return ResponseEntity.ok(Map.of("code", "00", "desc", "received", "error", e.getMessage()));
        }
        return ResponseEntity.ok(Map.of("code", "00", "desc", "success"));
    }
}
