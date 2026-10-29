package com.cinex.cinema.payment;

import com.cinex.cinema.booking.Booking;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Payment recordProcessing(Booking booking, String paymentMethod, BigDecimal amount, String transactionId) {
        Payment payment = paymentRepository.findByBookingId(booking.getId())
            .orElseGet(() -> Payment.builder().booking(booking).build());
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setPaymentMethod(paymentMethod);
        payment.setTransactionId(transactionId);
        payment.setPaidAt(null);
        return paymentRepository.save(payment);
    }

    public Payment recordSuccess(Booking booking, String paymentMethod, BigDecimal amount, String transactionId) {
        Payment payment = paymentRepository.findByBookingId(booking.getId())
            .orElseGet(() -> Payment.builder().booking(booking).build());
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentMethod(paymentMethod);
        if (transactionId != null) {
            payment.setTransactionId(transactionId);
        }
        payment.setPaidAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    public void refund(Booking booking) {
        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
        if (payment == null) {
            return;
        }
        // Chỉ đánh dấu hoàn tiền khi thực sự đã thu được tiền;
        // đơn chưa thanh toán bị huỷ thì coi giao dịch là thất bại.
        payment.setStatus(payment.getStatus() == PaymentStatus.SUCCESS
            ? PaymentStatus.REFUNDED
            : PaymentStatus.FAILED);
        paymentRepository.save(payment);
    }

    public void markSuccess(Booking booking, String paymentMethod, String transactionId) {
        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
        if (payment == null) return;
        payment.setStatus(PaymentStatus.SUCCESS);
        if (paymentMethod != null) payment.setPaymentMethod(paymentMethod);
        if (transactionId != null) payment.setTransactionId(transactionId);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }

    public Optional<Payment> findByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }

    public Optional<Payment> findByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId);
    }
}
