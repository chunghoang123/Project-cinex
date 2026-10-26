package com.cinex.cinema.admin;

import com.cinex.cinema.booking.Booking;
import com.cinex.cinema.booking.BookingMapper;
import com.cinex.cinema.booking.BookingRepository;
import com.cinex.cinema.booking.BookingResponse;
import com.cinex.cinema.booking.BookingService;
import com.cinex.cinema.booking.BookingStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminBookingService {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final BookingMapper bookingMapper;

    public AdminBookingService(BookingRepository bookingRepository, BookingService bookingService,
                               BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.bookingMapper = bookingMapper;
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> list(BookingStatus status, LocalDate date, String q) {
        List<Booking> bookings;
        if (q != null && !q.isBlank()) {
            String keyword = q.trim();
            bookings = bookingRepository
                .findByBookingCodeContainingIgnoreCaseOrCustomerPhoneContainingIgnoreCaseOrCustomerNameContainingIgnoreCase(
                    keyword, keyword, keyword);
        } else if (status != null) {
            bookings = bookingRepository.findByStatusOrderByCreatedAtDesc(status);
        } else if (date != null) {
            bookings = bookingRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        } else {
            bookings = bookingRepository.findAllByOrderByCreatedAtDesc();
        }

        return bookings.stream()
            .filter(item -> status == null || item.getStatus() == status)
            .filter(item -> date == null || item.getCreatedAt().toLocalDate().equals(date))
            .map(bookingMapper::toResponse)
            .collect(Collectors.toList());
    }

    public BookingResponse cancel(String bookingCode) {
        Booking booking = bookingService.cancelBooking(bookingCode);
        return bookingMapper.toResponse(booking);
    }
}
