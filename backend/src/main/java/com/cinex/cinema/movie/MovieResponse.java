package com.cinex.cinema.movie;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieResponse {
    private Long id;
    private Long tmdbId;
    private String title;
    private String description;
    private String posterUrl;
    private String trailerUrl;
    private Integer durationMinutes;
    private Integer ageRating;
    private String genre;
    private MovieStatus status;
    private LocalDate releaseDate;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}