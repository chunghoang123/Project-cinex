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

    public Payment recordSuccess(Booking booking, String paymentMethod, BigDecimal amount, String transactionId) {
        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElseGet(Payment::new);
        payment.setBooking(booking);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentMethod(paymentMethod == null || paymentMethod.isBlank() ? "ONLINE" : paymentMethod);
        payment.setTransactionId(transactionId);
        payment.setPaidAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    public Optional<Payment> findByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }

    public void refund(Booking booking) {
        paymentRepository.findByBookingId(booking.getId()).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        });
    }
}
