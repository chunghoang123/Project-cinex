package com.cinex.cinema.admin;

import com.cinex.cinema.cinema.AuditoriumRequest;
import com.cinex.cinema.cinema.AuditoriumResponse;
import com.cinex.cinema.cinema.AuditoriumService;
import com.cinex.cinema.cinema.CinemaRequest;
import com.cinex.cinema.cinema.CinemaResponse;
import com.cinex.cinema.cinema.CinemaService;
import com.cinex.cinema.cinema.SeatResponse;
import com.cinex.cinema.common.ApiResponse;
import com.cinex.cinema.movie.MovieRequest;
import com.cinex.cinema.movie.MovieResponse;
import com.cinex.cinema.movie.MovieService;
import com.cinex.cinema.showtime.ShowtimeRequest;
import com.cinex.cinema.showtime.ShowtimeResponse;
import com.cinex.cinema.showtime.ShowtimeService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCatalogController {

    private final MovieService movieService;
    private final CinemaService cinemaService;
    private final AuditoriumService auditoriumService;
    private final ShowtimeService showtimeService;

    public AdminCatalogController(MovieService movieService, CinemaService cinemaService,
                                  AuditoriumService auditoriumService, ShowtimeService showtimeService) {
        this.movieService = movieService;
        this.cinemaService = cinemaService;
        this.auditoriumService = auditoriumService;
        this.showtimeService = showtimeService;
    }

    @GetMapping("/movies")
    public ResponseEntity<ApiResponse<List<MovieResponse>>> movies() {
        return ResponseEntity.ok(ApiResponse.ok(movieService.findAllAdmin()));
    }

    @PostMapping("/movies")
    public ResponseEntity<ApiResponse<MovieResponse>> createMovie(@Valid @RequestBody MovieRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Tạo phim thành công", movieService.create(request)));
    }

    @PutMapping("/movies/{id}")
    public ResponseEntity<ApiResponse<MovieResponse>> updateMovie(@PathVariable Long id,
                                                                  @Valid @RequestBody MovieRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật phim thành công", movieService.update(id, request)));
    }

    @DeleteMapping("/movies/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMovie(@PathVariable Long id) {
        movieService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã ẩn phim", null));
    }

    @GetMapping("/cinemas")
    public ResponseEntity<ApiResponse<List<CinemaResponse>>> cinemas() {
        return ResponseEntity.ok(ApiResponse.ok(cinemaService.findAll(true)));
    }

    @PostMapping("/cinemas")
    public ResponseEntity<ApiResponse<CinemaResponse>> createCinema(@Valid @RequestBody CinemaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Tạo rạp thành công", cinemaService.create(request)));
    }

    @PutMapping("/cinemas/{id}")
    public ResponseEntity<ApiResponse<CinemaResponse>> updateCinema(@PathVariable Long id,
                                                                    @Valid @RequestBody CinemaRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật rạp thành công", cinemaService.update(id, request)));
    }

    @DeleteMapping("/cinemas/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCinema(@PathVariable Long id) {
        cinemaService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã ngừng hoạt động rạp", null));
    }

    @GetMapping("/cinemas/{cinemaId}/auditoriums")
    public ResponseEntity<ApiResponse<List<AuditoriumResponse>>> auditoriums(@PathVariable Long cinemaId) {
        return ResponseEntity.ok(ApiResponse.ok(auditoriumService.findByCinema(cinemaId, true)));
    }

    @PostMapping("/cinemas/{cinemaId}/auditoriums")
    public ResponseEntity<ApiResponse<AuditoriumResponse>> createAuditorium(
            @PathVariable Long cinemaId, @Valid @RequestBody AuditoriumRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Tạo phòng chiếu thành công",
                auditoriumService.create(cinemaId, request)));
    }

    @PutMapping("/auditoriums/{id}")
    public ResponseEntity<ApiResponse<AuditoriumResponse>> updateAuditorium(
            @PathVariable Long id, @Valid @RequestBody AuditoriumRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật phòng chiếu thành công",
            auditoriumService.update(id, request)));
    }

    @DeleteMapping("/auditoriums/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAuditorium(@PathVariable Long id) {
        auditoriumService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã ngừng hoạt động phòng chiếu", null));
    }

    @GetMapping("/auditoriums/{id}/seats")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> seats(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(auditoriumService.getSeats(id)));
    }

    @PostMapping("/auditoriums/{id}/seats/generate")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> generateSeats(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Đã tạo/cập nhật ghế",
            auditoriumService.generateSeats(id)));
    }

    @GetMapping("/showtimes")
    public ResponseEntity<ApiResponse<List<ShowtimeResponse>>> showtimes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(required = false) Long movieId) {
        return ResponseEntity.ok(ApiResponse.ok(showtimeService.findAdmin(date, cinemaId, movieId)));
    }

    @PostMapping("/showtimes")
    public ResponseEntity<ApiResponse<ShowtimeResponse>> createShowtime(
            @Valid @RequestBody ShowtimeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Tạo suất chiếu thành công", showtimeService.create(request)));
    }

    @PutMapping("/showtimes/{id}")
    public ResponseEntity<ApiResponse<ShowtimeResponse>> updateShowtime(
            @PathVariable Long id, @Valid @RequestBody ShowtimeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật suất chiếu thành công",
            showtimeService.update(id, request)));
    }

    @DeleteMapping("/showtimes/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteShowtime(@PathVariable Long id) {
        showtimeService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã hủy suất chiếu", null));
    }
}
