package com.cinex.cinema.payment;

import com.cinex.cinema.booking.BookingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
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
        return ResponseEntity.ok(processWebhookSafely(body));
    }

    private Map<String, Object> processWebhookSafely(Map<String, Object> body) {
        try {
            bookingService.handlePayOSWebhook(body);
            return Map.of("code", "00", "desc", "success");
        } catch (Exception e) {
            // PayOS yêu cầu trả HTTP 200 để không retry; ghi nhận lỗi trong log và payload
            log.warn("Xử lý webhook PayOS gặp lỗi: {}", e.getMessage());
            String errorMsg = e.getMessage() == null ? "unknown error" : e.getMessage();
            return Map.of("code", "00", "desc", "received", "error", errorMsg);
        }
    }
}
