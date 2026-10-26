package com.cinex.cinema.cinema;

import com.cinex.cinema.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cinemas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cinema extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 200)
    private String address;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false)
    private boolean active = true;
}