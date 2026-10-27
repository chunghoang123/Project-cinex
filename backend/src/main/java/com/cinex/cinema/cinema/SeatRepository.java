package com.cinex.cinema.cinema;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByAuditoriumIdAndActiveTrue(Long auditoriumId);
    List<Seat> findByAuditoriumId(Long auditoriumId);
    Optional<Seat> findByAuditoriumIdAndSeatCode(Long auditoriumId, String seatCode);
}