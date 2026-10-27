package com.cinex.cinema.staff;

import com.cinex.cinema.booking.BookingResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftSummaryResponse {
    private String date;
    private BigDecimal totalRevenue;
    private long ticketCount;
    private long transactionCount;
    private BigDecimal cashRevenue;
    private BigDecimal cardRevenue;
    private List<BookingResponse> recentSales;
}
