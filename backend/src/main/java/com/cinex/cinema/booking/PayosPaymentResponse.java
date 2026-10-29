package com.cinex.cinema.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Thông tin link thanh toán PayOS trả về cho khách hàng đặt vé online.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayosPaymentResponse {
    private String bookingCode;
    private String checkoutUrl;
    private String qrCode;
    private String paymentLinkId;
    private BigDecimal amount;
    private LocalDateTime expiresAt;
}
