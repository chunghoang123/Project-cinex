package com.cinex.cinema.movie;

import com.cinex.cinema.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MovieResponse>>> getAll(
            @RequestParam(required = false) MovieStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(movieService.findAll(status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(movieService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MovieResponse>> create(@Valid @RequestBody MovieRequest request) {
        MovieResponse response = movieService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Tạo phim thành công", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MovieResponse>> update(@PathVariable Long id,
                                                             @Valid @RequestBody MovieRequest request) {
        MovieResponse response = movieService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật phim thành công", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        movieService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa phim thành công", null));
    }
}