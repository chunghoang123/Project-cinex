package com.cinex.cinema.showtime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    List<Showtime> findByMovieIdAndStartTimeBetweenAndActiveTrue(Long movieId, LocalDateTime from, LocalDateTime to);
    List<Showtime> findByStartTimeBetweenAndActiveTrueOrderByStartTimeAsc(LocalDateTime from, LocalDateTime to);
    List<Showtime> findByAuditoriumCinemaIdAndStartTimeBetweenAndActiveTrueOrderByStartTimeAsc(
        Long cinemaId, LocalDateTime from, LocalDateTime to);
    List<Showtime> findByActiveTrueOrderByStartTimeAsc();
    List<Showtime> findByAuditoriumCinemaIdAndActiveTrueOrderByStartTimeAsc(Long cinemaId);
    List<Showtime> findByAuditoriumIdAndActiveTrue(Long auditoriumId);
    List<Showtime> findByMovieId(Long movieId);
    List<Showtime> findByStatusAndEndTimeBefore(ShowtimeStatus status, LocalDateTime time);
    long countByStartTimeBetweenAndActiveTrue(LocalDateTime from, LocalDateTime to);
}