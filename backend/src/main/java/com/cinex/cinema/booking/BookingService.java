package com.cinex.cinema.booking;

import com.cinex.cinema.common.SeatPricing;
import com.cinex.cinema.payment.PaymentService;
import com.cinex.cinema.showtime.SeatStatus;
import com.cinex.cinema.showtime.Showtime;
import com.cinex.cinema.showtime.ShowtimeRepository;
import com.cinex.cinema.showtime.ShowtimeSeat;
import com.cinex.cinema.showtime.ShowtimeSeatRepository;
import com.cinex.cinema.ticket.TicketService;
import com.cinex.cinema.user.User;
import com.cinex.cinema.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

    public BookingService(BookingRepository bookingRepository, BookingSeatRepository bookingSeatRepository,
                          ShowtimeSeatRepository showtimeSeatRepository, ShowtimeRepository showtimeRepository,
                          UserRepository userRepository, PaymentService paymentService,
                          TicketService ticketService) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.showtimeSeatRepository = showtimeSeatRepository;
        this.showtimeRepository = showtimeRepository;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
        this.ticketService = ticketService;
    }

    public Booking createHold(Long userId, Long showtimeId, List<Long> seatIds) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không tồn tại"));
        Showtime showtime = showtimeRepository.findById(showtimeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Suất chiếu không tồn tại"));

        if (!showtime.isActive() || showtime.getStartTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Suất chiếu không còn khả dụng");
        }

        List<ShowtimeSeat> seats = validateSeats(showtimeId, seatIds);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);
        String bookingCode = "BK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        for (ShowtimeSeat seat : seats) {
            seat.setStatus(SeatStatus.HELD);
            seat.setHeldUntil(expiresAt);
            seat.setHoldByBookingId(bookingCode);
        }
        showtimeSeatRepository.saveAll(seats);

        BigDecimal seatAmount = calculateSeatAmount(showtime, seats);
        Booking booking = Booking.builder()
            .bookingCode(bookingCode)
            .user(user)
            .showtime(showtime)
            .totalAmount(seatAmount)
            .seatAmount(seatAmount)
            .snackAmount(BigDecimal.ZERO)
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

    public Booking findByBookingCode(String bookingCode) {
        return bookingRepository.findByBookingCode(bookingCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn không tồn tại"));
    }

    public List<Booking> findMyBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Booking confirmBooking(String bookingCode) {
        return confirmBooking(bookingCode, "ONLINE");
    }

    public Booking confirmBooking(String bookingCode, String paymentMethod) {
        Booking booking = findByBookingCode(bookingCode);

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            paymentService.recordSuccess(booking, paymentMethod, booking.getTotalAmount(), "PAY-" + bookingCode);
            ticketService.issue(booking);
            return booking;
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn không ở trạng thái chờ thanh toán");
        }

        if (booking.getExpiresAt() != null && booking.getExpiresAt().isBefore(LocalDateTime.now())) {
            booking.setStatus(BookingStatus.EXPIRED);
            releaseSeats(booking.getBookingCode());
            bookingRepository.save(booking);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn đã hết hạn");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setExpiresAt(null);
        markSeatsBooked(booking.getBookingCode());
        booking = bookingRepository.save(booking);

        paymentService.recordSuccess(booking, paymentMethod, booking.getTotalAmount(), "PAY-" + bookingCode);
        ticketService.issue(booking);
        return booking;
    }

    public Booking payBooking(String bookingCode, String paymentMethod) {
        Booking booking = findByBookingCode(bookingCode);
        if (booking.getStatus() == BookingStatus.PENDING) {
            return confirmBooking(bookingCode, paymentMethod);
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn không thể thanh toán");
        }
        paymentService.recordSuccess(booking, paymentMethod, booking.getTotalAmount(), "PAY-" + bookingCode);
        ticketService.issue(booking);
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

    private void markSeatsBooked(String bookingCode) {
        List<ShowtimeSeat> seats = showtimeSeatRepository.findByHoldByBookingId(bookingCode);
        for (ShowtimeSeat seat : seats) {
            seat.setStatus(SeatStatus.BOOKED);
            seat.setHeldUntil(null);
        }
        showtimeSeatRepository.saveAll(seats);
    }

    private void releaseSeats(String bookingCode) {
        List<ShowtimeSeat> seats = showtimeSeatRepository.findByHoldByBookingId(bookingCode);
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
