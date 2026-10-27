package com.cinex.cinema.cinema;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditoriumRepository extends JpaRepository<Auditorium, Long> {
    List<Auditorium> findByCinemaIdAndActiveTrue(Long cinemaId);
    List<Auditorium> findByCinemaId(Long cinemaId);
}