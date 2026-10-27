package com.cinex.cinema.booking;

import com.cinex.cinema.cinema.Auditorium;
import com.cinex.cinema.cinema.Cinema;
import com.cinex.cinema.payment.Payment;
import com.cinex.cinema.payment.PaymentRepository;
import com.cinex.cinema.showtime.Showtime;
import com.cinex.cinema.ticket.Ticket;
import com.cinex.cinema.ticket.TicketRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BookingMapper {

    private final BookingSeatRepository bookingSeatRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;

    public BookingMapper(BookingSeatRepository bookingSeatRepository,
                         PaymentRepository paymentRepository,
                         TicketRepository ticketRepository) {
        this.bookingSeatRepository = bookingSeatRepository;
        this.paymentRepository = paymentRepository;
        this.ticketRepository = ticketRepository;
    }

    public BookingResponse toResponse(Booking booking) {
        Showtime showtime = booking.getShowtime();
        Auditorium auditorium = showtime.getAuditorium();
        Cinema cinema = auditorium.getCinema();

        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(booking.getId());
        List<BookingSeatResponse> seats = bookingSeats.stream()
            .map(item -> BookingSeatResponse.builder()
                .showtimeSeatId(item.getShowtimeSeat().getId())
                .seatId(item.getShowtimeSeat().getSeat().getId())
                .seatCode(item.getShowtimeSeat().getSeat().getSeatCode())
                .seatType(item.getShowtimeSeat().getSeat().getSeatType())
                .price(item.getPrice())
                .build())
            .collect(Collectors.toList());

        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
        Ticket ticket = ticketRepository.findByBookingId(booking.getId()).orElse(null);

        BigDecimal seatAmount = booking.getSeatAmount() != null
            ? booking.getSeatAmount()
            : seats.stream().map(BookingSeatResponse::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);

        return BookingResponse.builder()
            .id(booking.getId())
            .bookingCode(booking.getBookingCode())
            .customerName(booking.getCustomerName())
            .customerPhone(booking.getCustomerPhone())
            .channel(booking.getChannel() == null ? BookingChannel.ONLINE.name() : booking.getChannel().name())
            .status(booking.getStatus().name())
            .seatAmount(seatAmount)
            .snackAmount(booking.getSnackAmount() == null ? BigDecimal.ZERO : booking.getSnackAmount())
            .snackNote(booking.getSnackNote())
            .totalAmount(booking.getTotalAmount())
            .paymentMethod(payment == null ? null : payment.getPaymentMethod())
            .paymentStatus(payment == null ? null : payment.getStatus().name())
            .ticketCode(ticket == null ? null : ticket.getTicketCode())
            .ticketStatus(ticket == null ? null : ticket.getStatus().name())
            .createdAt(booking.getCreatedAt())
            .expiresAt(booking.getExpiresAt())
            .paidAt(payment == null ? null : payment.getPaidAt())
            .showtime(BookingShowtimeResponse.builder()
                .id(showtime.getId())
                .movieId(showtime.getMovie().getId())
                .movieTitle(showtime.getMovie().getTitle())
                .posterUrl(showtime.getMovie().getPosterUrl())
                .cinemaId(cinema.getId())
                .cinemaName(cinema.getName())
                .auditoriumName(auditorium.getName())
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .build())
            .seats(seats)
            .build();
    }
}
