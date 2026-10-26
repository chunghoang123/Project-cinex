package com.cinex.cinema.cinema;

import com.cinex.cinema.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cinemas")
public class CinemaController {
    private final CinemaService cinemaService;

    public CinemaController(CinemaService cinemaService) {
        this.cinemaService = cinemaService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CinemaResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(cinemaService.findAll(false)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CinemaResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(cinemaService.getResponse(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CinemaResponse>> create(@Valid @RequestBody CinemaRequest request) {
        CinemaResponse created = cinemaService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Tạo rạp thành công", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CinemaResponse>> update(@PathVariable Long id,
                                                              @Valid @RequestBody CinemaRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật rạp thành công",
            cinemaService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        cinemaService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa rạp thành công", null));
    }
}
