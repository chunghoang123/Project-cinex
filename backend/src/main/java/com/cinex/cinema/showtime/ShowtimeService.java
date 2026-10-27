package com.cinex.cinema.showtime;

import com.cinex.cinema.booking.BookingRepository;
import com.cinex.cinema.cinema.Auditorium;
import com.cinex.cinema.cinema.AuditoriumRepository;
import com.cinex.cinema.cinema.Seat;
import com.cinex.cinema.cinema.SeatRepository;
import com.cinex.cinema.common.SeatPricing;
import com.cinex.cinema.movie.Movie;
import com.cinex.cinema.movie.MovieRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ShowtimeService {
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
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ShowtimeResponse> findInRange(LocalDateTime from, LocalDateTime to) {
        return showtimeRepository.findByStartTimeBetweenAndActiveTrueOrderByStartTimeAsc(from, to)
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ShowtimeResponse> findAdmin(LocalDate date, Long cinemaId, Long movieId) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        LocalDateTime from = targetDate.atStartOfDay();
        LocalDateTime to = targetDate.plusDays(1).atStartOfDay();
        List<Showtime> showtimes = cinemaId == null
            ? showtimeRepository.findByStartTimeBetweenAndActiveTrueOrderByStartTimeAsc(from, to)
            : showtimeRepository.findByAuditoriumCinemaIdAndStartTimeBetweenAndActiveTrueOrderByStartTimeAsc(
                cinemaId, from, to);
        return showtimes.stream()
            .filter(item -> movieId == null || item.getMovie().getId().equals(movieId))
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public List<ShowtimeResponse> findForStaff(LocalDate date, Long cinemaId) {
        LocalDateTime now = LocalDateTime.now();
        return findAdmin(date, cinemaId, null).stream()
            .filter(item -> item.getStartTime().isAfter(now))
            .collect(Collectors.toList());
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
            .collect(Collectors.toList());
    }

    public ShowtimeResponse create(ShowtimeRequest request) {
        Movie movie = movieRepository.findById(request.getMovieId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Phim không tồn tại"));
        Auditorium auditorium = auditoriumRepository.findById(request.getAuditoriumId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Phòng chiếu không tồn tại"));
        if (!auditorium.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phòng chiếu đã ngừng hoạt động");
        }

        LocalDateTime endTime = request.getStartTime()
            .plusMinutes(movie.getDurationMinutes() == null ? 120 : movie.getDurationMinutes());
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
        if (bookingRepository.existsByShowtimeId(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Không thể sửa suất chiếu đã có đơn đặt vé");
        }
        Movie movie = movieRepository.findById(request.getMovieId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Phim không tồn tại"));
        Auditorium auditorium = auditoriumRepository.findById(request.getAuditoriumId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Phòng chiếu không tồn tại"));

        showtime.setMovie(movie);
        showtime.setAuditorium(auditorium);
        showtime.setStartTime(request.getStartTime());
        showtime.setEndTime(request.getStartTime()
            .plusMinutes(movie.getDurationMinutes() == null ? 120 : movie.getDurationMinutes()));
        showtime.setBasePrice(request.getBasePrice());
        showtime = showtimeRepository.save(showtime);

        showtimeSeatRepository.deleteAll(showtimeSeatRepository.findByShowtimeId(id));
        generateShowtimeSeats(showtime, auditorium);
        return toResponse(showtime);
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
            .collect(Collectors.toList());
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
