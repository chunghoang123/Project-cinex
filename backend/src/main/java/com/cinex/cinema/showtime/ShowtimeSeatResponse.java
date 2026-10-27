package com.cinex.cinema.showtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeSeatResponse {
    private Long id;
    private Long seatId;
    private String seatCode;
    private String seatType;
    private Integer rowNumber;
    private Integer columnNumber;
    private String status;
    private BigDecimal price;
}
