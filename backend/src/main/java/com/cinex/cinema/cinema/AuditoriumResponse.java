package com.cinex.cinema.cinema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriumResponse {
    private Long id;
    private String name;
    private Integer totalRows;
    private Integer columnsPerRow;
    private Long cinemaId;
    private String cinemaName;
    private boolean active;
    private long seatCount;
}
