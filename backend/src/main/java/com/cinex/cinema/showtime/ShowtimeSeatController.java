package com.cinex.cinema.showtime;

import com.cinex.cinema.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/showtimes")
public class ShowtimeSeatController {
    private final ShowtimeService showtimeService;

    public ShowtimeSeatController(ShowtimeService showtimeService) {
        this.showtimeService = showtimeService;
    }

    @GetMapping("/{showtimeId}/seats")
    public ResponseEntity<ApiResponse<List<ShowtimeSeatResponse>>> getSeats(@PathVariable Long showtimeId) {
        return ResponseEntity.ok(ApiResponse.ok(showtimeService.getSeatMap(showtimeId)));
    }
}
