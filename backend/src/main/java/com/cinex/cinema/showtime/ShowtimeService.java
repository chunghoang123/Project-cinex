package com.cinex.cinema.showtime;

import com.cinex.cinema.booking.BookingRepository;
import com.cinex.cinema.booking.BookingStatus;
import com.cinex.cinema.cinema.Auditorium;
import com.cinex.cinema.cinema.AuditoriumRepository;
import com.cinex.cinema.cinema.Seat;
import com.cinex.cinema.cinema.SeatRepository;
import com.cinex.cinema.common.SeatPricing;
import com.cinex.cinema.movie.Movie;
import com.cinex.cinema.movie.MovieRepository;
import com.cinex.cinema.movie.MovieStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class ShowtimeService {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final List<LocalTime> AUTO_SLOTS = List.of(
        LocalTime.of(10, 0),
        LocalTime.of(14, 30),
        LocalTime.of(19, 30)
    );

    public record AutoScheduleResult(
        int createdCount,
        int completedCount,
        LocalDate fromDate,
        LocalDate toDate,
        int daysAhead
    ) {}
    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final AuditoriumRepository auditoriumRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final BookingRepository bookingRepository;

    public ShowtimeService(ShowtimeRepository showtimeRepository, MovieRepository movieRepository,
                           AuditoriumRepository auditoriumRepository, SeatRepository seatRepository,
                           ShowtimeSeatRepository showtimeSeatRepository,
                           BookingRepository bookingRepository) {
        this.showtimeRepository = showtimeRepository;
        this.movieRepository = movieRepository;
        this.auditoriumRepository = auditoriumRepository;
        this.seatRepository = seatRepository;
        this.showtimeSeatRepository = showtimeSeatRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<ShowtimeResponse> findByMovieAndDate(Long movieId, LocalDateTime from, LocalDateTime to) {
        return showtimeRepository.findByMovieIdAndStartTimeBetweenAndActiveTrue(movieId, from, to)
            .stream()
            .filter(this::isBookable)
            .map(this::toResponse).toList();
    }

    public List<ShowtimeResponse> findInRange(LocalDateTime from, LocalDateTime to) {
        return showtimeRepository.findByStartTimeBetweenAndActiveTrueOrderByStartTimeAsc(from, to)
            .stream()
            .filter(this::isBookable)
            .map(this::toResponse).toList();
    }

    /** Phòng chiếu đang bảo trì hoặc rạp đã ngừng thì khách không thấy suất chiếu đó. */
    private boolean isBookable(Showtime showtime) {
        return showtime.getAuditorium().isActive() && showtime.getAuditorium().getCinema().isActive();
    }

    public List<ShowtimeResponse> findAdmin(LocalDate date, Long cinemaId, Long movieId) {
        List<Showtime> showtimes;
        if (date == null) {
            // Không lọc ngày: trả về toàn bộ suất chiếu còn hoạt động của rạp (hoặc toàn hệ thống)
            showtimes = cinemaId == null
                ? showtimeRepository.findByActiveTrueOrderByStartTimeAsc()
                : showtimeRepository.findByAuditoriumCinemaIdAndActiveTrueOrderByStartTimeAsc(cinemaId);
        } else {
            LocalDateTime from = date.atStartOfDay();
            LocalDateTime to = date.plusDays(1).atStartOfDay();
            showtimes = cinemaId == null
                ? showtimeRepository.findByStartTimeBetweenAndActiveTrueOrderByStartTimeAsc(from, to)
                : showtimeRepository.findByAuditoriumCinemaIdAndStartTimeBetweenAndActiveTrueOrderByStartTimeAsc(
                    cinemaId, from, to);
        }
        return showtimes.stream()
            .filter(item -> movieId == null || item.getMovie().getId().equals(movieId))
            .map(this::toResponse)
            .toList();
    }

    public List<ShowtimeResponse> findForStaff(LocalDate date, Long cinemaId) {
        LocalDateTime now = LocalDateTime.now();
        return findAdmin(date, cinemaId, null).stream()
            .filter(item -> item.getStartTime().isAfter(now))
            .toList();
    }

    public Showtime findById(Long id) {
        return showtimeRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Suất chiếu không tồn tại"));
    }

    public ShowtimeResponse getResponse(Long id) {
        return toResponse(findById(id));
    }

    public List<ShowtimeSeatResponse> getSeatMap(Long showtimeId) {
        Showtime showtime = findById(showtimeId);
        return showtimeSeatRepository.findByShowtimeId(showtimeId).stream()
            .sorted(Comparator.comparing((ShowtimeSeat item) -> item.getSeat().getRowNumber())
                .thenComparing(item -> item.getSeat().getColumnNumber()))
            .map(item -> toSeatResponse(item, showtime.getBasePrice()))
            .toList();
    }

    public ShowtimeResponse create(ShowtimeRequest request) {
        Movie movie = movieRepository.findById(request.getMovieId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Phim không tồn tại"));
        Auditorium auditorium = auditoriumRepository.findById(request.getAuditoriumId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Phòng chiếu không tồn tại"));
        if (!auditorium.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phòng chiếu đã ngừng hoạt động");
        }
        if (!auditorium.getCinema().isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rạp đã ngừng hoạt động");
        }

        LocalDateTime endTime = request.getStartTime()
            .plusMinutes(movie.getDurationMinutes() == null ? 120 : movie.getDurationMinutes());
        ensureNoOverlap(auditorium, request.getStartTime(), endTime, null);

        Showtime showtime = Showtime.builder()
            .movie(movie)
            .auditorium(auditorium)
            .startTime(request.getStartTime())
            .endTime(endTime)
            .basePrice(request.getBasePrice())
            .status(ShowtimeStatus.ACTIVE)
            .active(true)
            .build();
        showtime = showtimeRepository.save(showtime);
        generateShowtimeSeats(showtime, auditorium);
        return toResponse(showtime);
    }

    public ShowtimeResponse update(Long id, ShowtimeRequest request) {
        Showtime showtime = findById(id);
        // Chỉ chặn khi suất chiếu còn đơn "sống" (chờ thanh toán hoặc đã bán vé),
        // đơn đã hết hạn / đã hủy không cản trở việc chỉnh sửa.
        if (bookingRepository.existsByShowtimeIdAndStatusIn(
                id, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Suất chiếu đang chờ thanh toán hoặc đã bán vé, không thể cập nhật");
        }
        Movie movie = movieRepository.findById(request.getMovieId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Phim không tồn tại"));
        Auditorium auditorium = auditoriumRepository.findById(request.getAuditoriumId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Phòng chiếu không tồn tại"));
        if (!auditorium.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phòng chiếu đã ngừng hoạt động");
        }
        if (!auditorium.getCinema().isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rạp đã ngừng hoạt động");
        }

        LocalDateTime endTime = request.getStartTime()
            .plusMinutes(movie.getDurationMinutes() == null ? 120 : movie.getDurationMinutes());
        ensureNoOverlap(auditorium, request.getStartTime(), endTime, id);

        showtime.setMovie(movie);
        showtime.setAuditorium(auditorium);
        showtime.setStartTime(request.getStartTime());
        showtime.setEndTime(endTime);
        showtime.setBasePrice(request.getBasePrice());
        showtime = showtimeRepository.save(showtime);

        // Chỉ tái sinh sơ đồ ghế khi chưa có đơn nào từng đặt (tránh phá khoá ngoại
        // với booking_seats cũ). Đơn hết hạn/hủy vẫn giữ ShowtimeSeat để tra cứu lịch sử.
        if (!bookingRepository.existsByShowtimeId(id)) {
            showtimeSeatRepository.deleteAll(showtimeSeatRepository.findByShowtimeId(id));
            generateShowtimeSeats(showtime, auditorium);
        }
        return toResponse(showtime);
    }

    /**
     * Kiểm tra chống trùng giờ chiếu trong cùng một phòng chiếu.
     */
    private void ensureNoOverlap(Auditorium auditorium, LocalDateTime startTime,
                                 LocalDateTime endTime, Long excludeShowtimeId) {
        List<Showtime> existingList = showtimeRepository.findByAuditoriumIdAndActiveTrue(auditorium.getId());
        for (Showtime existing : existingList) {
            if (excludeShowtimeId != null && excludeShowtimeId.equals(existing.getId())) {
                continue;
            }
            if (existing.getStatus() == ShowtimeStatus.CANCELLED) {
                continue;
            }
            if (existing.getStartTime().isBefore(endTime) && existing.getEndTime().isAfter(startTime)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("Phòng %s (%s) đã có suất chiếu \"%s\" từ %s đến %s",
                        auditorium.getName(),
                        auditorium.getCinema().getName(),
                        existing.getMovie().getTitle(),
                        existing.getStartTime().format(TIME_FMT),
                        existing.getEndTime().format(TIME_FMT)));
            }
        }
    }

    /**
     * Tự động chuyển các suất chiếu đã kết thúc (endTime < now) từ ACTIVE sang COMPLETED.
     */
    public int completeFinishedShowtimes() {
        List<Showtime> finished = showtimeRepository.findByStatusAndEndTimeBefore(
            ShowtimeStatus.ACTIVE, LocalDateTime.now());
        for (Showtime showtime : finished) {
            showtime.setStatus(ShowtimeStatus.COMPLETED);
        }
        if (!finished.isEmpty()) {
            showtimeRepository.saveAll(finished);
        }
        return finished.size();
    }

    /**
     * Tự động thêm và lấp đầy lịch chiếu cho `daysAhead` ngày tới (mặc định 10 ngày).
     * Bỏ qua các khung giờ đã qua hoặc đã có suất chiếu trùng trong cùng phòng.
     */
    public AutoScheduleResult autoGenerateShowtimes(int daysAhead) {
        int safeDays = Math.max(1, Math.min(30, daysAhead <= 0 ? 10 : daysAhead));
        int completed = completeFinishedShowtimes();

        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(safeDays - 1L);

        List<Movie> movies = movieRepository.findByStatusAndActiveTrue(MovieStatus.NOW_SHOWING);
        List<Auditorium> auditoriums = findEligibleAuditoriums();
        if (movies.isEmpty() || auditoriums.isEmpty()) {
            return new AutoScheduleResult(0, completed, today, endDate, safeDays);
        }

        LocalDateTime cutoff = LocalDateTime.now().plusMinutes(10);
        int created = 0;

        for (int dayOffset = 0; dayOffset < safeDays; dayOffset++) {
            LocalDate date = today.plusDays(dayOffset);
            for (int audIndex = 0; audIndex < auditoriums.size(); audIndex++) {
                created += fillAuditoriumDaySlots(
                    auditoriums.get(audIndex),
                    movies,
                    date,
                    cutoff,
                    dayOffset + audIndex
                );
            }
        }

        return new AutoScheduleResult(created, completed, today, endDate, safeDays);
    }

    private List<Auditorium> findEligibleAuditoriums() {
        return auditoriumRepository.findAll().stream()
            .filter(a -> a.isActive() && a.getCinema() != null && a.getCinema().isActive())
            .filter(a -> !seatRepository.findByAuditoriumIdAndActiveTrue(a.getId()).isEmpty())
            .sorted(Comparator.comparing(Auditorium::getId))
            .toList();
    }

    private int fillAuditoriumDaySlots(Auditorium auditorium, List<Movie> movies,
                                       LocalDate date, LocalDateTime cutoff, int rotationSeed) {
        List<Showtime> existingInAud = new ArrayList<>(
            showtimeRepository.findByAuditoriumIdAndActiveTrue(auditorium.getId())
        );
        int created = 0;
        for (int slotIndex = 0; slotIndex < AUTO_SLOTS.size(); slotIndex++) {
            LocalTime slotTime = AUTO_SLOTS.get(slotIndex);
            LocalDateTime startTime = date.atTime(slotTime);
            if (!startTime.isAfter(cutoff)) {
                continue;
            }
            Movie movie = movies.get((rotationSeed + slotIndex) % movies.size());
            int duration = movie.getDurationMinutes() == null ? 120 : movie.getDurationMinutes();
            LocalDateTime endTime = startTime.plusMinutes(duration);

            if (hasActiveOverlap(existingInAud, startTime, endTime)) {
                continue;
            }

            Showtime showtime = showtimeRepository.save(Showtime.builder()
                .movie(movie)
                .auditorium(auditorium)
                .startTime(startTime)
                .endTime(endTime)
                .basePrice(resolveSlotPrice(slotTime))
                .status(ShowtimeStatus.ACTIVE)
                .active(true)
                .build());
            generateShowtimeSeats(showtime, auditorium);
            existingInAud.add(showtime);
            created++;
        }
        return created;
    }

    private static boolean hasActiveOverlap(List<Showtime> existingInAud,
                                            LocalDateTime startTime, LocalDateTime endTime) {
        return existingInAud.stream()
            .filter(ex -> ex.getStatus() != ShowtimeStatus.CANCELLED)
            .anyMatch(ex -> ex.getStartTime().isBefore(endTime) && ex.getEndTime().isAfter(startTime));
    }

    private static BigDecimal resolveSlotPrice(LocalTime slotTime) {
        return slotTime.getHour() >= 18 ? new BigDecimal("95000") : new BigDecimal("90000");
    }

    public void delete(Long id) {
        Showtime showtime = findById(id);
        showtime.setActive(false);
        showtime.setStatus(ShowtimeStatus.CANCELLED);
        showtimeRepository.save(showtime);
    }

    public void generateShowtimeSeats(Showtime showtime, Auditorium auditorium) {
        List<Seat> seats = seatRepository.findByAuditoriumIdAndActiveTrue(auditorium.getId());
        if (seats.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Phòng chiếu chưa có ghế. Vui lòng tạo ghế trước khi tạo suất chiếu");
        }
        List<ShowtimeSeat> showtimeSeats = seats.stream()
            .map(seat -> ShowtimeSeat.builder()
                .showtime(showtime)
                .seat(seat)
                .status(SeatStatus.AVAILABLE)
                .build())
            .toList();
        showtimeSeatRepository.saveAll(showtimeSeats);
    }

    public ShowtimeResponse toResponse(Showtime showtime) {
        Auditorium auditorium = showtime.getAuditorium();
        Movie movie = showtime.getMovie();
        long totalSeats = showtimeSeatRepository.countByShowtimeId(showtime.getId());
        long availableSeats = showtimeSeatRepository.countByShowtimeIdAndStatus(
            showtime.getId(), SeatStatus.AVAILABLE);
        return ShowtimeResponse.builder()
            .id(showtime.getId())
            .movieId(movie.getId())
            .movieTitle(movie.getTitle())
            .posterUrl(movie.getPosterUrl())
            .durationMinutes(movie.getDurationMinutes())
            .cinemaId(auditorium.getCinema().getId())
            .cinemaName(auditorium.getCinema().getName())
            .auditoriumId(auditorium.getId())
            .auditoriumName(auditorium.getName())
            .startTime(showtime.getStartTime())
            .endTime(showtime.getEndTime())
            .basePrice(showtime.getBasePrice())
            .status(showtime.getStatus().name())
            .active(showtime.isActive())
            .availableSeats(availableSeats)
            .totalSeats(totalSeats)
            .build();
    }

    public ShowtimeSeatResponse toSeatResponse(ShowtimeSeat item, BigDecimal basePrice) {
        Seat seat = item.getSeat();
        return ShowtimeSeatResponse.builder()
            .id(item.getId())
            .seatId(seat.getId())
            .seatCode(seat.getSeatCode())
            .seatType(seat.getSeatType())
            .rowNumber(seat.getRowNumber())
            .columnNumber(seat.getColumnNumber())
            .status(item.getStatus().name())
            .price(SeatPricing.priceFor(basePrice, seat.getSeatType()))
            .build();
    }
}
