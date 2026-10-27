package com.cinex.cinema.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingShowtimeResponse {
    private Long id;
    private Long movieId;
    private String movieTitle;
    private String posterUrl;
    private Long cinemaId;
    private String cinemaName;
    private String auditoriumName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
