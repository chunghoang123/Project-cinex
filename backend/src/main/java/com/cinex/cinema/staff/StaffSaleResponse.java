package com.cinex.cinema.staff;

import com.cinex.cinema.booking.BookingResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffSaleResponse {
    private BookingResponse booking;
    private String ticketCode;
    private String qrCode;
    private String checkoutUrl;
    private String paymentLinkId;
}
