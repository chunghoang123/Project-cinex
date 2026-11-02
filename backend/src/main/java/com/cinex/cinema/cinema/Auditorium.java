package com.cinex.cinema.cinema;

import com.cinex.cinema.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "auditoriums")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auditorium extends BaseEntity {
    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private Integer totalRows;

    @Column(nullable = false)
    private Integer columnsPerRow;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cinema_id", nullable = false)
    private Cinema cinema;

    @Column(nullable = false)
    private boolean active = true;
}