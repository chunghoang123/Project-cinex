package com.cinex.cinema.config;

import com.cinex.cinema.booking.Booking;
import com.cinex.cinema.booking.BookingChannel;
import com.cinex.cinema.booking.BookingRepository;
import com.cinex.cinema.booking.BookingSeat;
import com.cinex.cinema.booking.BookingSeatRepository;
import com.cinex.cinema.booking.BookingStatus;
import com.cinex.cinema.cinema.Auditorium;
import com.cinex.cinema.cinema.AuditoriumRepository;
import com.cinex.cinema.cinema.AuditoriumService;
import com.cinex.cinema.cinema.Cinema;
import com.cinex.cinema.cinema.CinemaRepository;
import com.cinex.cinema.common.SeatPricing;
import com.cinex.cinema.movie.Movie;
import com.cinex.cinema.movie.MovieRepository;
import com.cinex.cinema.movie.MovieStatus;
import com.cinex.cinema.payment.Payment;
import com.cinex.cinema.payment.PaymentRepository;
import com.cinex.cinema.payment.PaymentStatus;
import com.cinex.cinema.showtime.SeatStatus;
import com.cinex.cinema.showtime.Showtime;
import com.cinex.cinema.showtime.ShowtimeRepository;
import com.cinex.cinema.showtime.ShowtimeSeat;
import com.cinex.cinema.showtime.ShowtimeSeatRepository;
import com.cinex.cinema.showtime.ShowtimeService;
import com.cinex.cinema.showtime.ShowtimeStatus;
import com.cinex.cinema.ticket.Ticket;
import com.cinex.cinema.ticket.TicketRepository;
import com.cinex.cinema.ticket.TicketStatus;
import com.cinex.cinema.user.User;
import com.cinex.cinema.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Order(3)
public class CatalogSeeder implements CommandLineRunner {

    private static final String TMDB = "https://image.tmdb.org/t/p/w500";

    private final CinemaRepository cinemaRepository;
    private final AuditoriumRepository auditoriumRepository;
    private final AuditoriumService auditoriumService;
    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final ShowtimeService showtimeService;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public CatalogSeeder(CinemaRepository cinemaRepository, AuditoriumRepository auditoriumRepository,
                         AuditoriumService auditoriumService, MovieRepository movieRepository,
                         ShowtimeRepository showtimeRepository, ShowtimeSeatRepository showtimeSeatRepository,
                         ShowtimeService showtimeService, BookingRepository bookingRepository,
                         BookingSeatRepository bookingSeatRepository, PaymentRepository paymentRepository,
                         TicketRepository ticketRepository, UserRepository userRepository) {
        this.cinemaRepository = cinemaRepository;
        this.auditoriumRepository = auditoriumRepository;
        this.auditoriumService = auditoriumService;
        this.movieRepository = movieRepository;
        this.showtimeRepository = showtimeRepository;
        this.showtimeSeatRepository = showtimeSeatRepository;
        this.showtimeService = showtimeService;
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.paymentRepository = paymentRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (cinemaRepository.count() == 0) {
            seedCinemas();
        }
        if (movieRepository.count() == 0) {
            seedMovies();
        }
        if (showtimeRepository.count() == 0) {
            seedShowtimes();
        }
        if (bookingRepository.count() == 0) {
            seedBookings();
        }
    }

    private void seedCinemas() {
        Cinema landmark = cinemaRepository.save(Cinema.builder()
            .name("Cinex Landmark 81")
            .address("720A Điện Biên Phủ, Bình Thạnh, TP.HCM")
            .phone("1900 6017")
            .active(true)
            .build());
        Cinema galaxy = cinemaRepository.save(Cinema.builder()
            .name("Cinex Galaxy Nguyễn Du")
            .address("116 Nguyễn Du, Quận 1, TP.HCM")
            .phone("1900 6018")
            .active(true)
            .build());
        Cinema aeon = cinemaRepository.save(Cinema.builder()
            .name("Cinex Aeon Tân Phú")
            .address("30 Bờ Bao Tân Thắng, Tân Phú, TP.HCM")
            .phone("1900 6019")
            .active(true)
            .build());

        for (Cinema cinema : List.of(landmark, galaxy, aeon)) {
            for (int index = 1; index <= 2; index++) {
                Auditorium auditorium = auditoriumRepository.save(Auditorium.builder()
                    .name("Screen " + index)
                    .totalRows(8)
                    .columnsPerRow(12)
                    .cinema(cinema)
                    .active(true)
                    .build());
                auditoriumService.generateSeats(auditorium);
            }
        }
    }

    private void seedMovies() {
        movieRepository.saveAll(List.of(
            movie("Dune: Part Two", "Paul Atreides hợp lực cùng người Fremen để trả thù và bảo vệ hành tinh Arrakis.",
                TMDB + "/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg", 166, 13, "Sci-Fi, Adventure",
                MovieStatus.NOW_SHOWING),
            movie("Blade Runner 2049", "Một thợ săn replicant phát hiện bí mật có thể thay đổi tương lai loài người.",
                TMDB + "/gajva2L0rPYkEWjzgFlBXCAVBE5.jpg", 164, 16, "Sci-Fi, Thriller",
                MovieStatus.NOW_SHOWING),
            movie("Civil War", "Một nhóm nhà báo đi xuyên nước Mỹ trong bối cảnh nội chiến hiện đại.",
                TMDB + "/sh7Rg8Er3tFcN9BpKIPOMvALgZd.jpg", 109, 18, "Action, Thriller",
                MovieStatus.NOW_SHOWING),
            movie("The Creator", "Cuộc chiến giữa loài người và trí tuệ nhân tạo qua góc nhìn một cựu đặc vụ.",
                TMDB + "/vBZ0qvaRxqEhZwl6LWmruJqWE8Z.jpg", 133, 16, "Sci-Fi, Drama",
                MovieStatus.NOW_SHOWING),
            movie("Interstellar", "Những nhà thám hiểm du hành qua hố đen để tìm ngôi nhà mới cho nhân loại.",
                TMDB + "/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg", 169, 13, "Sci-Fi, Drama",
                MovieStatus.NOW_SHOWING),
            movie("The Batman", "Batman truy lùng Riddler và khám phá bóng tối của thành phố Gotham.",
                TMDB + "/74xTEgt7R36Fpooo50r9T25onhq.jpg", 176, 16, "Action, Crime",
                MovieStatus.NOW_SHOWING),
            movie("Spider-Man: Across the Spider-Verse", "Miles Morales bước vào cuộc phiêu lưu đa vũ trụ đầy màu sắc.",
                TMDB + "/8Vt6mWEReuy4Of61Lnj5Xj704m8.jpg", 140, 13, "Animation, Action",
                MovieStatus.NOW_SHOWING),
            movie("Oppenheimer", "Chân dung nhà vật lý J. Robert Oppenheimer và dự án Manhattan.",
                TMDB + "/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg", 180, 16, "Drama, History",
                MovieStatus.COMING_SOON)
        ));
    }

    private Movie movie(String title, String description, String posterUrl, int duration,
                        int ageRating, String genre, MovieStatus status) {
        return Movie.builder()
            .title(title)
            .description(description)
            .posterUrl(posterUrl)
            .trailerUrl("https://www.youtube.com")
            .durationMinutes(duration)
            .ageRating(ageRating)
            .genre(genre)
            .status(status)
            .active(true)
            .build();
    }

    private void seedShowtimes() {
        List<Movie> movies = movieRepository.findAll().stream()
            .filter(movie -> movie.getStatus() == MovieStatus.NOW_SHOWING && movie.isActive())
            .limit(2)
            .collect(Collectors.toList());
        List<Cinema> cinemas = cinemaRepository.findAll();
        LocalDateTime base = LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0);
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<LocalDateTime> startTimes = new ArrayList<>(List.of(
            base,
            base.plusHours(3),
            base.plusHours(6),
            tomorrow.atTime(10, 0),
            tomorrow.atTime(14, 0),
            tomorrow.atTime(19, 30)
        ));

        for (Movie movie : movies) {
            for (Cinema cinema : cinemas) {
                List<Auditorium> auditoriums = auditoriumRepository.findByCinemaIdAndActiveTrue(cinema.getId());
                for (Auditorium auditorium : auditoriums) {
                    for (LocalDateTime startTime : startTimes) {
                        Showtime showtime = showtimeRepository.save(Showtime.builder()
                            .movie(movie)
                            .auditorium(auditorium)
                            .startTime(startTime)
                            .endTime(startTime.plusMinutes(movie.getDurationMinutes()))
                            .basePrice(new BigDecimal("90000"))
                            .status(ShowtimeStatus.ACTIVE)
                            .active(true)
                            .build());
                        showtimeService.generateShowtimeSeats(showtime, auditorium);
                    }
                }
            }
        }
    }

    private void seedBookings() {
        User customer = userRepository.findByUsername("customer").orElse(null);
        User staff = userRepository.findByUsername("staff").orElse(null);
        if (customer == null || staff == null) {
            return;
        }

        List<Showtime> showtimes = showtimeRepository.findAll().stream()
            .filter(Showtime::isActive)
            .collect(Collectors.toList());
        if (showtimes.isEmpty()) {
            return;
        }

        for (int index = 0; index < 10; index++) {
            Showtime showtime = showtimes.get(index % showtimes.size());
            List<ShowtimeSeat> seats = showtimeSeatRepository.findByShowtimeId(showtime.getId()).stream()
                .filter(seat -> seat.getStatus() == SeatStatus.AVAILABLE)
                .limit(2)
                .collect(Collectors.toList());
            if (seats.size() < 2) {
                continue;
            }

            boolean counter = index >= 7;
            String bookingCode = String.format("BKDEMO%04d", index + 1);
            BigDecimal seatAmount = seats.stream()
                .map(seat -> SeatPricing.priceFor(showtime.getBasePrice(), seat.getSeat().getSeatType()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal snackAmount = counter && index == 9 ? new BigDecimal("65000") : BigDecimal.ZERO;
            BigDecimal totalAmount = seatAmount.add(snackAmount);

            Booking booking = bookingRepository.save(Booking.builder()
                .bookingCode(bookingCode)
                .user(customer)
                .showtime(showtime)
                .totalAmount(totalAmount)
                .seatAmount(seatAmount)
                .snackAmount(snackAmount)
                .snackNote(snackAmount.signum() > 0 ? "1 Combo bắp nước" : null)
                .customerName(counter ? "Khách tại quầy" : "Minh Dương")
                .customerPhone(counter ? "0909999999" : "0901234567")
                .channel(counter ? BookingChannel.COUNTER : BookingChannel.ONLINE)
                .staff(counter ? staff : null)
                .status(BookingStatus.CONFIRMED)
                .expiresAt(null)
                .build());

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

            paymentRepository.save(Payment.builder()
                .booking(booking)
                .transactionId("DEMO-" + bookingCode)
                .amount(totalAmount)
                .status(PaymentStatus.SUCCESS)
                .paymentMethod(counter ? "CASH" : (index % 2 == 0 ? "MOMO" : "CARD"))
                .paidAt(LocalDateTime.now().minusHours(index))
                .build());

            String ticketCode = String.format("TKDEMO%04d", index + 1);
            ticketRepository.save(Ticket.builder()
                .booking(booking)
                .ticketCode(ticketCode)
                .qrCode(ticketCode)
                .status(TicketStatus.UNUSED)
                .build());
        }
    }
}
