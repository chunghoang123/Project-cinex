package com.cinex.cinema.showtime;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, Long> {
    List<ShowtimeSeat> findByShowtimeId(Long showtimeId);
    List<ShowtimeSeat> findByHoldByBookingId(String holdByBookingId);
    long countByShowtimeId(Long showtimeId);
    long countByShowtimeIdAndStatus(Long showtimeId, SeatStatus status);

    /** Khoá ghi các ghế khi chốt thanh toán để tránh hai đơn cùng chiếm một ghế. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ShowtimeSeat s where s.id in :ids")
    List<ShowtimeSeat> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);

    /** Khoá ghi các ghế đang gắn với một đơn trước khi nhả ghế. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ShowtimeSeat s where s.holdByBookingId = :code")
    List<ShowtimeSeat> findByHoldByBookingIdForUpdate(@Param("code") String code);
}