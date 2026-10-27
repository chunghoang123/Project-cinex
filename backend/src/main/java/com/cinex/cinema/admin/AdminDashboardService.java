package com.cinex.cinema.admin;

import com.cinex.cinema.booking.Booking;
import com.cinex.cinema.booking.BookingMapper;
import com.cinex.cinema.booking.BookingRepository;
import com.cinex.cinema.booking.BookingResponse;
import com.cinex.cinema.booking.BookingSeatRepository;
import com.cinex.cinema.payment.Payment;
import com.cinex.cinema.payment.PaymentRepository;
import com.cinex.cinema.payment.PaymentStatus;
import com.cinex.cinema.showtime.ShowtimeRepository;
import com.cinex.cinema.ticket.TicketRepository;
import com.cinex.cinema.user.ERole;
import com.cinex.cinema.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final UserRepository userRepository;
    private final ShowtimeRepository showtimeRepository;
    private final BookingMapper bookingMapper;

    public AdminDashboardService(PaymentRepository paymentRepository, TicketRepository ticketRepository,
                                 BookingRepository bookingRepository, BookingSeatRepository bookingSeatRepository,
                                 UserRepository userRepository, ShowtimeRepository showtimeRepository,
                                 BookingMapper bookingMapper) {
        this.paymentRepository = paymentRepository;
        this.ticketRepository = ticketRepository;
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.userRepository = userRepository;
        this.showtimeRepository = showtimeRepository;
        this.bookingMapper = bookingMapper;
    }

    public AdminDashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        List<Payment> todayPayments = paymentRepository.findByStatusAndPaidAtBetween(
            PaymentStatus.SUCCESS, todayStart, tomorrowStart);
        BigDecimal revenueToday = todayPayments.stream()
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long ticketsToday = ticketRepository.findByCreatedAtBetween(todayStart, tomorrowStart).size();
        long bookingsToday = bookingRepository.countByCreatedAtBetween(todayStart, tomorrowStart);
        long customers = userRepository.countByRolesName(ERole.ROLE_CUSTOMER);
        long showtimesToday = showtimeRepository.countByStartTimeBetweenAndActiveTrue(todayStart, tomorrowStart);

        List<AdminDashboardResponse.DailyRevenue> revenueLast7Days = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime from = day.atStartOfDay();
            LocalDateTime to = day.plusDays(1).atStartOfDay();
            BigDecimal revenue = paymentRepository.findByStatusAndPaidAtBetween(
                    PaymentStatus.SUCCESS, from, to)
                .stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            revenueLast7Days.add(AdminDashboardResponse.DailyRevenue.builder()
                .date(day.format(formatter))
                .revenue(revenue)
                .build());
        }

        List<Payment> recentPayments = paymentRepository.findByStatusAndPaidAtAfter(
            PaymentStatus.SUCCESS, today.minusDays(30).atStartOfDay());
        Map<Long, AdminDashboardResponse.TopMovie> movieStats = new LinkedHashMap<>();
        for (Payment payment : recentPayments) {
            Booking booking = payment.getBooking();
            Long movieId = booking.getShowtime().getMovie().getId();
            String title = booking.getShowtime().getMovie().getTitle();
            long seatCount = bookingSeatRepository.findByBookingId(booking.getId()).size();
            AdminDashboardResponse.TopMovie current = movieStats.getOrDefault(movieId,
                AdminDashboardResponse.TopMovie.builder()
                    .movieId(movieId)
                    .title(title)
                    .tickets(0)
                    .revenue(BigDecimal.ZERO)
                    .build());
            current.setTickets(current.getTickets() + seatCount);
            current.setRevenue(current.getRevenue().add(payment.getAmount()));
            movieStats.put(movieId, current);
        }
        List<AdminDashboardResponse.TopMovie> topMovies = movieStats.values().stream()
            .sorted(Comparator.comparing(AdminDashboardResponse.TopMovie::getRevenue).reversed())
            .limit(5)
            .collect(Collectors.toList());

        List<BookingResponse> recentBookings = bookingRepository.findTop8ByOrderByCreatedAtDesc()
            .stream().map(bookingMapper::toResponse).collect(Collectors.toList());

        return AdminDashboardResponse.builder()
            .revenueToday(revenueToday)
            .ticketsToday(ticketsToday)
            .bookingsToday(bookingsToday)
            .customers(customers)
            .showtimesToday(showtimesToday)
            .revenueLast7Days(revenueLast7Days)
            .topMovies(topMovies)
            .recentBookings(recentBookings)
            .build();
    }
}
