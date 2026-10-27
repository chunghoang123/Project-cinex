package com.cinex.cinema.showtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeResponse {
    private Long id;
    private Long movieId;
    private String movieTitle;
    private String posterUrl;
    private Integer durationMinutes;
    private Long cinemaId;
    private String cinemaName;
    private Long auditoriumId;
    private String auditoriumName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal basePrice;
    private String status;
    private boolean active;
    private long availableSeats;
    private long totalSeats;
}
