package com.cinex.cinema.cinema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {
    private Long id;
    private String seatCode;
    private String seatType;
    private Integer rowNumber;
    private Integer columnNumber;
    private boolean active;
}
