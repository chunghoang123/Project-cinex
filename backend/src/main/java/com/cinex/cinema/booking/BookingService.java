package com.cinex.cinema.booking;

import com.cinex.cinema.common.SeatPricing;
import com.cinex.cinema.payment.PayOSService;
import com.cinex.cinema.payment.Payment;
import com.cinex.cinema.payment.PaymentService;
import com.cinex.cinema.payment.PaymentStatus;
import com.cinex.cinema.showtime.SeatStatus;
import com.cinex.cinema.showtime.Showtime;
import com.cinex.cinema.showtime.ShowtimeRepository;
import com.cinex.cinema.showtime.ShowtimeSeat;
import com.cinex.cinema.showtime.ShowtimeSeatRepository;
import com.cinex.cinema.ticket.TicketService;
import com.cinex.cinema.user.User;
import com.cinex.cinema.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final TicketService ticketService;
    private final PayOSService payOSService;

    @Value("${payos.return-url:http://localhost:5173/payment/return}")
    private String payosReturnUrl;

    @Value("${payos.cancel-url:http://localhost:5173/payment/cancel}")
    private String payosCancelUrl;

    public BookingService(BookingRepository bookingRepository, BookingSeatRepository bookingSeatRepository,
                          ShowtimeSeatRepository showtimeSeatRepository, ShowtimeRepository showtimeRepository,
                          UserRepository userRepository,
                          @org.springframework.context.annotation.Lazy PaymentService paymentService,
                          @org.springframework.context.annotation.Lazy TicketService ticketService,
                          PayOSService payOSService) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.showtimeSeatRepository = showtimeSeatRepository;
        this.showtimeRepository = showtimeRepository;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
        this.ticketService = ticketService;
        this.payOSService = payOSService;
    }

    public Booking createHold(Long userId, Long showtimeId, List<Long> seatIds) {
        return createHold(userId, showtimeId, seatIds, BigDecimal.ZERO, null);
    }

    public Booking createHold(Long userId, Long showtimeId, List<Long> seatIds,
                              BigDecimal snackAmount, String snackNote) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không tồn tại"));
        Showtime showtime = showtimeRepository.findById(showtimeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Suất chiếu không tồn tại"));

        if (!showtime.isActive() || showtime.getStartTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Suất chiếu không còn khả dụng");
        }

        BigDecimal extraAmount = snackAmount == null ? BigDecimal.ZERO : snackAmount;
        if (extraAmount.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tiền bắp nước không hợp lệ");
        }

        List<ShowtimeSeat> seats = validateSeats(showtimeId, seatIds);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);
        String bookingCode = "BK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Không khoá ghế ở bước tạo đơn: ghế chỉ chuyển BOOKED khi PayOS xác nhận đã nhận tiền.
        // Nhờ vậy khách không giữ chỗ trước khi trả tiền; xung đột hiếm được xử lý lúc chốt đơn.
        showtimeSeatRepository.saveAll(seats);

        BigDecimal seatAmount = calculateSeatAmount(showtime, seats);
        Booking booking = Booking.builder()
            .bookingCode(bookingCode)
            .user(user)
            .showtime(showtime)
            .totalAmount(seatAmount.add(extraAmount))
            .seatAmount(seatAmount)
            .snackAmount(extraAmount)
            .snackNote(snackNote)
            .customerName(user.getFullName() == null ? user.getUsername() : user.getFullName())
            .customerPhone(user.getPhone())
            .channel(BookingChannel.ONLINE)
            .status(BookingStatus.PENDING)
            .expiresAt(expiresAt)
            .build();
        booking = bookingRepository.save(booking);

        for (ShowtimeSeat seat : seats) {
            bookingSeatRepository.save(BookingSeat.builder()
                .booking(booking)
                .showtimeSeat(seat)
                .price(SeatPricing.priceFor(showtime.getBasePrice(), seat.getSeat().getSeatType()))
                .build());
        }

        return booking;
    }

    public List<Booking> findByUserId(Long userId) {
        return bookingRepository.findAll().stream()
            .filter(b -> b.getUser().getId().equals(userId))
            .collect(Collectors.toList());
    }

    public List<Booking> findMyBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Booking findByBookingCode(String bookingCode) {
        return bookingRepository.findByBookingCode(bookingCode)
            .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Đơn không tồn tại"));
    }

    /**
     * Tạo (hoặc lấy lại) link thanh toán PayOS cho đơn đặt vé online.
     * Ghế không bị khoá trước khi trả tiền, chỉ kiểm tra còn trống tại thời điểm tạo link.
     */
    public PayosPaymentResponse createPayosPayment(String bookingCode) {
        Booking booking = findByBookingCode(bookingCode);
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn đã được thanh toán");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn không ở trạng thái chờ thanh toán");
        }
        if (booking.getExpiresAt() != null && booking.getExpiresAt().isBefore(LocalDateTime.now())) {
            booking.setStatus(BookingStatus.EXPIRED);
            releaseSeats(booking.getBookingCode());
            bookingRepository.save(booking);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn đã hết hạn thanh toán");
        }
        ensureSeatsFree(booking);

        // Tái sử dụng link còn hiệu lực để tránh tạo trùng orderCode bên PayOS
        Payment existing = paymentService.findByBookingId(booking.getId()).orElse(null);
        if (existing != null && existing.getStatus() == PaymentStatus.PROCESSING
            && existing.getTransactionId() != null) {
            PayOSService.PayOSLink current = payOSService.getPaymentLink(existing.getTransactionId());
            if (current != null) {
                return toPayosResponse(booking, current);
            }
        }

        PayOSService.PayOSLink link = payOSService.createPaymentLink(
            PayOSService.generateOrderCode(),
            booking.getTotalAmount().longValue(),
            "Cinex " + booking.getBookingCode(),
            withBookingCode(payosReturnUrl, booking.getBookingCode()),
            withBookingCode(payosCancelUrl, booking.getBookingCode())
        );
        paymentService.recordProcessing(booking, "PAYOS", booking.getTotalAmount(), link.paymentLinkId);
        return toPayosResponse(booking, link);
    }

    /** Gắn mã đơn vào URL return/cancel để trang kết quả biết đơn nào kể cả khi mở tab mới. */
    private String withBookingCode(String url, String bookingCode) {
        if (url == null || url.isBlank()) {
            return url;
        }
        return url + (url.contains("?") ? "&" : "?") + "code=" + bookingCode;
    }

    private PayosPaymentResponse toPayosResponse(Booking booking, PayOSService.PayOSLink link) {
        return PayosPaymentResponse.builder()
            .bookingCode(booking.getBookingCode())
            .checkoutUrl(link.checkoutUrl)
            .qrCode(link.qrCode)
            .paymentLinkId(link.paymentLinkId)
            .amount(booking.getTotalAmount())
            .expiresAt(booking.getExpiresAt())
            .build();
    }

    /**
     * Xử lý webhook PayOS: xác minh chữ ký, xác nhận đơn và phát hành vé.
     */
    @SuppressWarnings("unchecked")
    public void handlePayOSWebhook(Map<String, Object> body) {
        if (!payOSService.verifyWebhookSignature(body)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chữ ký webhook PayOS không hợp lệ");
        }
        if (!"00".equals(String.valueOf(body.get("code")))) {
            return;
        }
        Object dataObj = body.get("data");
        if (!(dataObj instanceof Map)) {
            return;
        }
        Map<String, Object> data = (Map<String, Object>) dataObj;

        // Ưu tiên tra theo paymentLinkId (không phụ thuộc orderCode do PayOS sinh)
        Object linkIdObj = data.get("paymentLinkId");
        String paymentLinkId = linkIdObj == null ? null : String.valueOf(linkIdObj);
        if (paymentLinkId != null) {
            Payment payment = paymentService.findByTransactionId(paymentLinkId).orElse(null);
            if (payment != null && payment.getBooking() != null) {
                confirmPayosBooking(payment.getBooking().getId(), paymentLinkId);
                return;
            }
        }

        // Dự phòng cho các link cũ dùng orderCode = booking.id
        long orderCode;
        try {
            orderCode = Long.parseLong(String.valueOf(data.get("orderCode")));
        } catch (NumberFormatException e) {
            return;
        }
        confirmPayosBooking(orderCode, paymentLinkId);
    }

    /**
     * Chốt đơn sau khi PayOS xác nhận đã nhận tiền (idempotent).
     * Ghế chỉ chuyển BOOKED ở bước này; nếu ghế đã bị đơn khác mua trước thì huỷ đơn
     * và ghi nhận thanh toán để hoàn tiền thủ công.
     */
    public void confirmPayosBooking(Long bookingId, String paymentLinkId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return;
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            paymentService.markSuccess(booking, "PAYOS", paymentLinkId);
            return;
        }
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.EXPIRED) {
            return;
        }

        List<Long> seatIds = bookingSeatRepository.findByBookingId(booking.getId()).stream()
            .map(bookingSeat -> bookingSeat.getShowtimeSeat().getId())
            .toList();
        // Khoá ghi để hai đơn trả tiền cùng lúc không thể cùng chiếm một ghế
        List<ShowtimeSeat> seats = seatIds.isEmpty()
            ? List.of()
            : showtimeSeatRepository.findAllByIdForUpdate(seatIds);
        LocalDateTime now = LocalDateTime.now();
        boolean seatsAvailable = !seats.isEmpty() && seats.stream().allMatch(seat ->
            seat.getStatus() == SeatStatus.AVAILABLE
                || (seat.getStatus() == SeatStatus.HELD
                    && seat.getHeldUntil() != null && seat.getHeldUntil().isAfter(now)
                    && booking.getBookingCode().equals(seat.getHoldByBookingId()))
                || (seat.getStatus() == SeatStatus.BOOKED
                    && booking.getBookingCode().equals(seat.getHoldByBookingId())));
        if (!seatsAvailable) {
            // Đã nhận tiền nhưng ghế không còn trống: huỷ đơn, cần hoàn tiền thủ công
            paymentService.markSuccess(booking, "PAYOS", paymentLinkId);
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setExpiresAt(null);
            bookingRepository.save(booking);
            return;
        }

        for (ShowtimeSeat seat : seats) {
            seat.setStatus(SeatStatus.BOOKED);
            seat.setHeldUntil(null);
            seat.setHoldByBookingId(booking.getBookingCode());
        }
        showtimeSeatRepository.saveAll(seats);

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setExpiresAt(null);
        bookingRepository.save(booking);

        paymentService.markSuccess(booking, "PAYOS", paymentLinkId);
        ticketService.issue(booking);
    }

    /**
     * Đồng bộ trạng thái thanh toán trực tiếp từ PayOS.
     * Dùng khi webhook chưa tới (ví dụ môi trường local không có URL public).
     */
    public Booking syncPayosPayment(String bookingCode) {
        Booking booking = findByBookingCode(bookingCode);
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return booking;
        }
        Payment payment = paymentService.findByBookingId(booking.getId()).orElse(null);
        if (payment == null || payment.getTransactionId() == null) {
            return booking;
        }
        if ("PAID".equals(payOSService.getPaymentStatus(payment.getTransactionId()))) {
            confirmPayosBooking(booking.getId(), payment.getTransactionId());
            return findByBookingCode(bookingCode);
        }
        return booking;
    }

    public Booking cancelBooking(String bookingCode) {
        Booking booking = findByBookingCode(bookingCode);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return booking;
        }

        booking.setStatus(BookingStatus.CANCELLED);
        releaseSeats(bookingCode);
        paymentService.refund(booking);
        ticketService.cancel(booking);
        return bookingRepository.save(booking);
    }

    public boolean isOwner(Booking booking, Long userId) {
        return booking.getUser() != null && booking.getUser().getId().equals(userId);
    }

    /** Kiểm tra ghế của đơn còn trống trước khi tạo link thanh toán. */
    private void ensureSeatsFree(Booking booking) {
        LocalDateTime now = LocalDateTime.now();
        List<ShowtimeSeat> seats = bookingSeatRepository.findByBookingId(booking.getId()).stream()
            .map(BookingSeat::getShowtimeSeat)
            .collect(Collectors.toList());
        for (ShowtimeSeat seat : seats) {
            boolean takenByOther = seat.getStatus() == SeatStatus.BOOKED
                || (seat.getStatus() == SeatStatus.HELD
                    && seat.getHeldUntil() != null && seat.getHeldUntil().isAfter(now)
                    && !booking.getBookingCode().equals(seat.getHoldByBookingId()));
            if (takenByOther) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ghế " + seat.getSeat().getSeatCode() + " đã được người khác đặt");
            }
        }
    }

    private List<ShowtimeSeat> validateSeats(Long showtimeId, List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ít nhất một ghế");
        }

        List<ShowtimeSeat> seats = showtimeSeatRepository.findAllById(seatIds);
        if (seats.size() != seatIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Một số ghế không tồn tại");
        }

        LocalDateTime now = LocalDateTime.now();
        for (ShowtimeSeat seat : seats) {
            if (!seat.getShowtime().getId().equals(showtimeId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ghế không thuộc suất chiếu này");
            }
            if (seat.getStatus() == SeatStatus.BOOKED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ghế " + seat.getSeat().getSeatCode() + " đã được đặt");
            }
            if (seat.getStatus() == SeatStatus.HELD
                && seat.getHeldUntil() != null && seat.getHeldUntil().isAfter(now)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ghế " + seat.getSeat().getSeatCode() + " đang được giữ");
            }
            if (seat.getStatus() == SeatStatus.HELD) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setHeldUntil(null);
                seat.setHoldByBookingId(null);
            }
        }
        return seats;
    }

    private BigDecimal calculateSeatAmount(Showtime showtime, List<ShowtimeSeat> seats) {
        return seats.stream()
            .map(seat -> SeatPricing.priceFor(showtime.getBasePrice(), seat.getSeat().getSeatType()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void releaseSeats(String bookingCode) {
        List<ShowtimeSeat> seats = showtimeSeatRepository.findByHoldByBookingIdForUpdate(bookingCode);
        for (ShowtimeSeat seat : seats) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setHeldUntil(null);
            seat.setHoldByBookingId(null);
        }
        showtimeSeatRepository.saveAll(seats);
    }

    @Scheduled(fixedRate = 60000)
    public void releaseExpiredHolds() {
        List<Booking> expired = bookingRepository.findByStatusAndExpiresAtBefore(
            BookingStatus.PENDING, LocalDateTime.now());
        for (Booking booking : expired) {
            booking.setStatus(BookingStatus.EXPIRED);
            releaseSeats(booking.getBookingCode());
            bookingRepository.save(booking);
        }
    }
}
