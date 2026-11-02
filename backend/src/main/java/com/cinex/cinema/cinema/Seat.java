package com.cinex.cinema.cinema;

import com.cinex.cinema.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat extends BaseEntity {
    @Column(nullable = false, length = 10)
    private String seatCode;

    @Column(nullable = false, length = 20)
    private String seatType;

    // MySQL 8 chặn tên cột trùng từ khoá window function (row_number/column_number)
    // nên đặt tên cột tường minh, an toàn
    @Column(name = "row_num", nullable = false)
    private Integer rowNumber;

    @Column(name = "column_num", nullable = false)
    private Integer columnNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditorium_id", nullable = false)
    private Auditorium auditorium;

    @Column(nullable = false)
    private boolean active = true;
}