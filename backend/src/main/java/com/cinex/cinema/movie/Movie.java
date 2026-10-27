package com.cinex.cinema.movie;

import com.cinex.cinema.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "movies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie extends BaseEntity {
    /** id của phim trên TMDB, dùng để đồng bộ dữ liệu không trùng lặp */
    @Column(name = "tmdb_id", unique = true)
    private Long tmdbId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(length = 2000)
    private String description;

    @Column(length = 500)
    private String posterUrl;

    @Column(length = 500)
    private String trailerUrl;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "age_rating")
    private Integer ageRating;

    @Column(length = 100)
    private String genre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovieStatus status;

    @Column(nullable = false)
    private boolean active = true;
}