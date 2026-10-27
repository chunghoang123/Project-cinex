package com.cinex.cinema.admin;

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
public class AdminDashboardResponse {
    private BigDecimal revenueToday;
    private long ticketsToday;
    private long bookingsToday;
    private long customers;
    private long showtimesToday;
    private List<DailyRevenue> revenueLast7Days;
    private List<TopMovie> topMovies;
    private List<BookingResponse> recentBookings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRevenue {
        private String date;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopMovie {
        private Long movieId;
        private String title;
        private long tickets;
        private BigDecimal revenue;
    }
}
