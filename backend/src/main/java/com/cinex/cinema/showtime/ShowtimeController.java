package com.cinex.cinema.showtime;

import com.cinex.cinema.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/showtimes")
public class ShowtimeController {
    private final ShowtimeService showtimeService;

    public ShowtimeController(ShowtimeService showtimeService) {
        this.showtimeService = showtimeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShowtimeResponse>>> search(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        LocalDateTime fromDate = from == null ? LocalDateTime.now() : LocalDateTime.parse(from);
        LocalDateTime toDate = to == null
            ? fromDate.plusDays(7)
            : LocalDateTime.parse(to);
        List<ShowtimeResponse> result = movieId == null
            ? showtimeService.findInRange(fromDate, toDate)
            : showtimeService.findByMovieAndDate(movieId, fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShowtimeResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(showtimeService.getResponse(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShowtimeResponse>> create(@Valid @RequestBody ShowtimeRequest request) {
        ShowtimeResponse created = showtimeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Tạo suất chiếu thành công", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShowtimeResponse>> update(@PathVariable Long id,
                                                                @Valid @RequestBody ShowtimeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật suất chiếu thành công",
            showtimeService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        showtimeService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Hủy suất chiếu thành công", null));
    }
}
