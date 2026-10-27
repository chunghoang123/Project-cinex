package com.cinex.cinema.staff;

import com.cinex.cinema.booking.Booking;
import com.cinex.cinema.booking.BookingChannel;
import com.cinex.cinema.booking.BookingMapper;
import com.cinex.cinema.booking.BookingRepository;
import com.cinex.cinema.booking.BookingResponse;
import com.cinex.cinema.booking.BookingSeat;
import com.cinex.cinema.booking.BookingSeatRepository;
import com.cinex.cinema.booking.BookingService;
import com.cinex.cinema.booking.BookingStatus;
import com.cinex.cinema.common.SeatPricing;
import com.cinex.cinema.payment.Payment;
import com.cinex.cinema.payment.PaymentService;
import com.cinex.cinema.showtime.SeatStatus;
import com.cinex.cinema.showtime.Showtime;
import com.cinex.cinema.showtime.ShowtimeRepository;
import com.cinex.cinema.showtime.ShowtimeSeat;
import com.cinex.cinema.showtime.ShowtimeSeatRepository;
import com.cinex.cinema.showtime.ShowtimeSeatResponse;
import com.cinex.cinema.showtime.ShowtimeResponse;
import com.cinex.cinema.showtime.ShowtimeService;
import com.cinex.cinema.ticket.Ticket;
import com.cinex.cinema.ticket.TicketService;
import com.cinex.cinema.user.User;
import com.cinex.cinema.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class StaffService {

    private static final Set<String> PAYMENT_METHODS = Set.of("CASH", "CARD", "TRANSFER");

    private final UserRepository userRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingService bookingService;
    private final BookingMapper bookingMapper;
    private final ShowtimeService showtimeService;
    private final PaymentService paymentService;
    private final TicketService ticketService;

    public StaffService(UserRepository userRepository, ShowtimeRepository showtimeRepository,
                        ShowtimeSeatRepository showtimeSeatRepository, BookingRepository bookingRepository,
                        BookingSeatRepository bookingSeatRepository, BookingService bookingService,
                        BookingMapper bookingMapper, ShowtimeService showtimeService,
                        PaymentService paymentService, TicketService ticketService) {
        this.userRepository = userRepository;
        this.showtimeRepository = showtimeRepository;
        this.showtimeSeatRepository = showtimeSeatRepository;
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.bookingService = bookingService;
        this.bookingMapper = bookingMapper;
        this.showtimeService = showtimeService;
        this.paymentService = paymentService;
        this.ticketService = ticketService;
    }

    @Transactional(readOnly = true)
    public List<ShowtimeResponse> listShowtimes(LocalDate date, Long cinemaId) {
        return showtimeService.findForStaff(date, cinemaId);
    }

    @Transactional(readOnly = true)
    public List<ShowtimeSeatResponse> getSeats(Long showtimeId) {
        return showtimeService.getSeatMap(showtimeId);
    }

    public StaffSaleResponse createSale(Long staffId, StaffSaleRequest request) {
        User staff = userRepository.findById(staffId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nhân viên không tồn tại"));
        User walkIn = userRepository.findByUsername("walkin")
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, "Chưa có tài khoản khách vãng lai"));

        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Suất chiếu không tồn tại"));
        if (!showtime.isActive() || showtime.getStartTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Suất chiếu không còn khả dụng");
        }

        List<ShowtimeSeat> seats = showtimeSeatRepository.findAllById(request.getSeatIds());
        if (seats.size() != request.getSeatIds().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Một số ghế không tồn tại");
        }
        validateSeats(showtime, seats);

        String paymentMethod = request.getPaymentMethod() == null
            ? "CASH" : request.getPaymentMethod().toUpperCase();
        if (!PAYMENT_METHODS.contains(paymentMethod)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phương thức thanh toán không hợp lệ");
        }

        BigDecimal snackAmount = request.getSnackAmount() == null
            ? BigDecimal.ZERO : request.getSnackAmount();
        if (snackAmount.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tiền bắp nước không hợp lệ");
        }

        BigDecimal seatAmount = seats.stream()
            .map(seat -> SeatPricing.priceFor(showtime.getBasePrice(), seat.getSeat().getSeatType()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAmount = seatAmount.add(snackAmount);

        String bookingCode = "BK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String customerName = request.getCustomerName() == null || request.getCustomerName().isBlank()
            ? "Khách lẻ" : request.getCustomerName().trim();

        Booking booking = Booking.builder()
            .bookingCode(bookingCode)
            .user(walkIn)
            .showtime(showtime)
            .totalAmount(totalAmount)
            .seatAmount(seatAmount)
            .snackAmount(snackAmount)
            .snackNote(request.getSnackNote())
            .customerName(customerName)
            .customerPhone(request.getCustomerPhone())
            .channel(BookingChannel.COUNTER)
            .staff(staff)
            .status(BookingStatus.CONFIRMED)
            .expiresAt(null)
            .build();
        booking = bookingRepository.save(booking);

        for (ShowtimeSeat seat : seats) {
            seat.setStatus(SeatStatus.BOOKED);
            seat.setHeldUntil(null);
            seat.setHoldByBookingId(bookingCode);
            bookingSeatRepository.save(BookingSeat.builder()
                .booking(booking)
                .showtimeSeat(seat)
                .price(SeatPricing.priceFor(showtime.getBasePrice(), seat.getSeat().getSeatType()))
                .build());
        }
        showtimeSeatRepository.saveAll(seats);

        paymentService.recordSuccess(booking, paymentMethod, totalAmount, "POS-" + bookingCode);
        Ticket ticket = ticketService.issue(booking);

        return StaffSaleResponse.builder()
            .booking(bookingMapper.toResponse(booking))
            .ticketCode(ticket.getTicketCode())
            .qrCode(ticket.getQrCode())
            .build();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> search(String q) {
        List<Booking> bookings;
        if (q == null || q.isBlank()) {
            bookings = bookingRepository.findAllByOrderByCreatedAtDesc();
        } else {
            String keyword = q.trim();
            bookings = bookingRepository
                .findByBookingCodeContainingIgnoreCaseOrCustomerPhoneContainingIgnoreCaseOrCustomerNameContainingIgnoreCase(
                    keyword, keyword, keyword);
        }
        return bookings.stream().map(bookingMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(String code) {
        return bookingMapper.toResponse(bookingService.findByBookingCode(code));
    }

    public BookingResponse cancel(String code) {
        return bookingMapper.toResponse(bookingService.cancelBooking(code));
    }

    public TicketValidationResponse validateTicket(String code) {
        try {
            Ticket ticket = ticketService.validate(code);
            return TicketValidationResponse.builder()
                .valid(true)
                .message("Vé hợp lệ")
                .ticketCode(ticket.getTicketCode())
                .ticketStatus(ticket.getStatus().name())
                .scannedAt(ticket.getScannedAt())
                .booking(bookingMapper.toResponse(ticket.getBooking()))
                .build();
        } catch (ResponseStatusException ex) {
            return TicketValidationResponse.builder()
                .valid(false)
                .message(ex.getReason())
                .ticketCode(code)
                .build();
        }
    }

    @Transactional(readOnly = true)
    public ShiftSummaryResponse shiftSummary(LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        LocalDateTime from = targetDate.atStartOfDay();
        LocalDateTime to = targetDate.plusDays(1).atStartOfDay();

        List<Booking> counterBookings = bookingRepository
            .findByCreatedAtBetweenOrderByCreatedAtDesc(from, to).stream()
            .filter(booking -> booking.getChannel() == BookingChannel.COUNTER)
            .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
            .collect(Collectors.toList());

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal cashRevenue = BigDecimal.ZERO;
        BigDecimal cardRevenue = BigDecimal.ZERO;
        long ticketCount = 0;

        for (Booking booking : counterBookings) {
            totalRevenue = totalRevenue.add(booking.getTotalAmount());
            Payment payment = paymentService.findByBookingId(booking.getId()).orElse(null);
            if (payment != null) {
                if ("CASH".equalsIgnoreCase(payment.getPaymentMethod())) {
                    cashRevenue = cashRevenue.add(payment.getAmount());
                } else {
                    cardRevenue = cardRevenue.add(payment.getAmount());
                }
            }
            ticketCount += bookingSeatRepository.findByBookingId(booking.getId()).size();
        }

        return ShiftSummaryResponse.builder()
            .date(targetDate.toString())
            .totalRevenue(totalRevenue)
            .ticketCount(ticketCount)
            .transactionCount(counterBookings.size())
            .cashRevenue(cashRevenue)
            .cardRevenue(cardRevenue)
            .recentSales(counterBookings.stream().limit(10)
                .map(bookingMapper::toResponse).collect(Collectors.toList()))
            .build();
    }

    private void validateSeats(Showtime showtime, List<ShowtimeSeat> seats) {
        LocalDateTime now = LocalDateTime.now();
        for (ShowtimeSeat seat : seats) {
            if (!seat.getShowtime().getId().equals(showtime.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ghế không thuộc suất chiếu này");
            }
            if (seat.getStatus() == SeatStatus.BOOKED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ghế " + seat.getSeat().getSeatCode() + " đã được bán");
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
    }
}
