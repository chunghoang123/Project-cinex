package com.cinex.cinema.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSeatResponse {
    private Long showtimeSeatId;
    private Long seatId;
    private String seatCode;
    private String seatType;
    private BigDecimal price;
}
