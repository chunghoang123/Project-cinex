package com.cinex.cinema.showtime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, Long> {
    List<ShowtimeSeat> findByShowtimeId(Long showtimeId);
    List<ShowtimeSeat> findByHoldByBookingId(String holdByBookingId);
    long countByShowtimeId(Long showtimeId);
    long countByShowtimeIdAndStatus(Long showtimeId, SeatStatus status);
}