package com.cinex.cinema.core;

import com.cinex.cinema.auth.AuthResponse;
import com.cinex.cinema.auth.AuthService;
import com.cinex.cinema.auth.ChangePasswordRequest;
import com.cinex.cinema.auth.EmailService;
import com.cinex.cinema.auth.JwtService;
import com.cinex.cinema.auth.LoginRequest;
import com.cinex.cinema.auth.PasswordResetResponse;
import com.cinex.cinema.auth.RegisterRequest;
import com.cinex.cinema.booking.Booking;
import com.cinex.cinema.booking.BookingChannel;
import com.cinex.cinema.booking.BookingMapper;
import com.cinex.cinema.booking.BookingRepository;
import com.cinex.cinema.booking.BookingResponse;
import com.cinex.cinema.booking.BookingSeat;
import com.cinex.cinema.booking.BookingSeatRepository;
import com.cinex.cinema.booking.BookingService;
import com.cinex.cinema.booking.BookingStatus;
import com.cinex.cinema.booking.PayosPaymentResponse;
import com.cinex.cinema.cinema.Auditorium;
import com.cinex.cinema.cinema.Cinema;
import com.cinex.cinema.cinema.Seat;
import com.cinex.cinema.movie.Movie;
import com.cinex.cinema.movie.MovieStatus;
import com.cinex.cinema.payment.PayOSService;
import com.cinex.cinema.payment.Payment;
import com.cinex.cinema.payment.PaymentRepository;
import com.cinex.cinema.payment.PaymentService;
import com.cinex.cinema.payment.PaymentStatus;
import com.cinex.cinema.showtime.SeatStatus;
import com.cinex.cinema.showtime.Showtime;
import com.cinex.cinema.showtime.ShowtimeRepository;
import com.cinex.cinema.showtime.ShowtimeSeat;
import com.cinex.cinema.showtime.ShowtimeSeatRepository;
import com.cinex.cinema.showtime.ShowtimeService;
import com.cinex.cinema.showtime.ShowtimeStatus;
import com.cinex.cinema.staff.ShiftSummaryResponse;
import com.cinex.cinema.staff.StaffSaleRequest;
import com.cinex.cinema.staff.StaffSaleResponse;
import com.cinex.cinema.staff.StaffService;
import com.cinex.cinema.staff.TicketValidationResponse;
import com.cinex.cinema.ticket.Ticket;
import com.cinex.cinema.ticket.TicketRepository;
import com.cinex.cinema.ticket.TicketService;
import com.cinex.cinema.ticket.TicketStatus;
import com.cinex.cinema.ticket.TicketValidator;
import com.cinex.cinema.user.ERole;
import com.cinex.cinema.user.Role;
import com.cinex.cinema.user.RoleRepository;
import com.cinex.cinema.user.User;
import com.cinex.cinema.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Bộ kiểm thử đơn vị cho các luồng nghiệp vụ cốt lõi theo thứ tự ưu tiên:
 * Booking -> Payment -> Ticket -> Staff -> Auth.
 */
class CoreServicesUnitTest {

    // ======================== 1. BOOKING SERVICE ========================

    @Test
    void bookingCreateHoldCalculatesTotalAndRejectsBookedSeats() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        BookingSeatRepository bookingSeatRepository = mock(BookingSeatRepository.class);
        ShowtimeSeatRepository showtimeSeatRepository = mock(ShowtimeSeatRepository.class);
        ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        PaymentService paymentService = mock(PaymentService.class);
        TicketService ticketService = mock(TicketService.class);
        PayOSService payOSService = mock(PayOSService.class);

        BookingService bookingService = new BookingService(
            bookingRepository, bookingSeatRepository, showtimeSeatRepository,
            showtimeRepository, userRepository, paymentService, ticketService, payOSService
        );

        User user = User.builder().username("customer").fullName("Minh Dương").phone("0901234567").build();
        user.setId(1L);
        Movie movie = Movie.builder().title("Dune").status(MovieStatus.NOW_SHOWING).build();
        Cinema cinema = Cinema.builder().name("Landmark 81").build();
        Auditorium auditorium = Auditorium.builder().name("Screen 1").cinema(cinema).build();
        Showtime showtime = Showtime.builder()
            .movie(movie)
            .auditorium(auditorium)
            .startTime(LocalDateTime.now().plusHours(4))
            .endTime(LocalDateTime.now().plusHours(6))
            .basePrice(new BigDecimal("100000"))
            .status(ShowtimeStatus.ACTIVE)
            .active(true)
            .build();
        showtime.setId(10L);

        Seat seatA1 = Seat.builder().seatCode("A1").seatType("STANDARD").rowNumber(1).columnNumber(1).build();
        ShowtimeSeat ss1 = ShowtimeSeat.builder().showtime(showtime).seat(seatA1).status(SeatStatus.AVAILABLE).build();
        ss1.setId(101L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(showtimeRepository.findById(10L)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findAllById(List.of(101L))).thenReturn(List.of(ss1));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(500L);
            return b;
        });

        Booking created = bookingService.createHold(1L, 10L, List.of(101L), new BigDecimal("50000"), "1 Combo");
        assertThat(created.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(created.getSeatAmount()).isEqualByComparingTo("100000");
        assertThat(created.getTotalAmount()).isEqualByComparingTo("150000");

        // Khi ghế đã BOOKED thì từ chối tạo đơn mới
        ss1.setStatus(SeatStatus.BOOKED);
        List<Long> seatIds = List.of(101L);
        assertThatThrownBy(() -> bookingService.createHold(1L, 10L, seatIds))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("đã được đặt");
    }

    @Test
    void bookingConfirmPayosLocksSeatsAndIssuesTicketAndCancelReleasesExpiredBooking() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        BookingSeatRepository bookingSeatRepository = mock(BookingSeatRepository.class);
        ShowtimeSeatRepository showtimeSeatRepository = mock(ShowtimeSeatRepository.class);
        ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        PaymentService paymentService = mock(PaymentService.class);
        TicketService ticketService = mock(TicketService.class);
        PayOSService payOSService = mock(PayOSService.class);

        BookingService bookingService = new BookingService(
            bookingRepository, bookingSeatRepository, showtimeSeatRepository,
            showtimeRepository, userRepository, paymentService, ticketService, payOSService
        );

        Showtime showtime = Showtime.builder().basePrice(new BigDecimal("90000")).build();
        showtime.setId(10L);
        Seat seat = Seat.builder().seatCode("B2").seatType("VIP").build();
        ShowtimeSeat ss = ShowtimeSeat.builder().showtime(showtime).seat(seat).status(SeatStatus.AVAILABLE).build();
        ss.setId(202L);

        Booking pendingBooking = Booking.builder()
            .bookingCode("BKTEST01")
            .showtime(showtime)
            .totalAmount(new BigDecimal("110000"))
            .status(BookingStatus.PENDING)
            .expiresAt(LocalDateTime.now().plusMinutes(8))
            .build();
        pendingBooking.setId(77L);

        BookingSeat bs = BookingSeat.builder().booking(pendingBooking).showtimeSeat(ss).price(new BigDecimal("110000")).build();

        when(bookingRepository.findById(77L)).thenReturn(Optional.of(pendingBooking));
        when(bookingSeatRepository.findByBookingId(77L)).thenReturn(List.of(bs));
        when(showtimeSeatRepository.findAllByIdForUpdate(List.of(202L))).thenReturn(List.of(ss));
        when(payOSService.createPaymentLink(anyLong(), anyLong(), anyString(), any(), any()))
            .thenReturn(new PayOSService.PayOSLink("https://pay.payos.vn/web/plink1", "QR_DATA", "plink1"));
        when(bookingRepository.findByBookingCode("BKTEST01")).thenReturn(Optional.of(pendingBooking));

        PayosPaymentResponse payosResp = bookingService.createPayosPayment("BKTEST01");
        assertThat(payosResp.getPaymentLinkId()).isEqualTo("plink1");

        // Chốt đơn khi nhận webhook PayOS
        when(payOSService.verifyWebhookSignature(any())).thenReturn(true);
        Payment paymentRecord = Payment.builder().booking(pendingBooking).transactionId("plink1").status(PaymentStatus.PROCESSING).build();
        when(paymentService.findByTransactionId("plink1")).thenReturn(Optional.of(paymentRecord));

        bookingService.handlePayOSWebhook(Map.of(
            "code", "00",
            "data", Map.of("paymentLinkId", "plink1", "orderCode", 77L),
            "signature", "valid"
        ));

        assertThat(pendingBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(ss.getStatus()).isEqualTo(SeatStatus.BOOKED);
        verify(ticketService).issue(pendingBooking);

        // Hủy đơn đã CONFIRMED -> giải phóng ghế, hoàn tiền, hủy vé
        when(showtimeSeatRepository.findByHoldByBookingIdForUpdate("BKTEST01")).thenReturn(List.of(ss));
        when(bookingRepository.save(pendingBooking)).thenReturn(pendingBooking);

        Booking cancelled = bookingService.cancelBooking("BKTEST01");
        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(ss.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        verify(paymentService).refund(pendingBooking);
        verify(ticketService).cancel(pendingBooking);
    }

    // ======================== 2. PAYMENT SERVICE ========================

    @Test
    void paymentServiceRecordsProcessingSuccessAndRefundCorrectly() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        PaymentService paymentService = new PaymentService(paymentRepository);

        Booking booking = Booking.builder().bookingCode("BKPay01").build();
        booking.setId(15L);

        when(paymentRepository.findByBookingId(15L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment processing = paymentService.recordProcessing(booking, "PAYOS", new BigDecimal("180000"), "plink_15");
        assertThat(processing.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(processing.getPaidAt()).isNull();

        when(paymentRepository.findByBookingId(15L)).thenReturn(Optional.of(processing));
        paymentService.refund(booking);
        // Đơn chưa thu tiền (PROCESSING) khi hủy sẽ chuyển sang FAILED chứ không phải REFUNDED
        assertThat(processing.getStatus()).isEqualTo(PaymentStatus.FAILED);

        Payment paid = paymentService.recordSuccess(booking, "CASH", new BigDecimal("180000"), "POS-15");
        assertThat(paid.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(paid.getPaidAt()).isNotNull();

        paymentService.refund(booking);
        assertThat(paid.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    // ======================== 3. TICKET SERVICE & VALIDATOR ========================

    @Test
    void ticketValidatorAndServiceIssueNormalizeQrAndValidateLifecycle() {
        TicketRepository ticketRepository = mock(TicketRepository.class);
        TicketValidator ticketValidator = new TicketValidator(ticketRepository);
        TicketService ticketService = new TicketService(ticketRepository, ticketValidator);

        Booking booking = Booking.builder().bookingCode("BKQR99").build();
        booking.setId(99L);

        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket issued = ticketService.issue(booking);
        assertThat(issued.getTicketCode()).startsWith("TKT-");
        assertThat(issued.getQrCode()).isEqualTo("CINEX|" + issued.getTicketCode() + "|BKQR99");
        assertThat(issued.getStatus()).isEqualTo(TicketStatus.UNUSED);

        when(ticketRepository.findByTicketCode(issued.getTicketCode())).thenReturn(Optional.of(issued));

        // Quét bằng chuỗi QR đầy đủ CINEX|TKT-...|BK...
        Ticket scanned = ticketService.validate(issued.getQrCode());
        assertThat(scanned.getStatus()).isEqualTo(TicketStatus.SCANNED);
        assertThat(scanned.getScannedAt()).isNotNull();

        // Quét lại lần 2 phải báo lỗi vé đã được soát
        String ticketCode = issued.getTicketCode();
        assertThatThrownBy(() -> ticketService.validateTicket(ticketCode))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Vé đã được soát");
    }

    // ======================== 4. STAFF SERVICE ========================

    @Test
    void staffServiceCreatesCounterSaleAndComputesShiftSummary() {
        UserRepository userRepository = mock(UserRepository.class);
        ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
        ShowtimeSeatRepository showtimeSeatRepository = mock(ShowtimeSeatRepository.class);
        BookingRepository bookingRepository = mock(BookingRepository.class);
        BookingSeatRepository bookingSeatRepository = mock(BookingSeatRepository.class);
        BookingService bookingService = mock(BookingService.class);
        BookingMapper bookingMapper = mock(BookingMapper.class);
        ShowtimeService showtimeService = mock(ShowtimeService.class);
        PaymentService paymentService = mock(PaymentService.class);
        TicketService ticketService = mock(TicketService.class);
        PayOSService payOSService = mock(PayOSService.class);

        StaffService staffService = new StaffService(
            userRepository, showtimeRepository, showtimeSeatRepository,
            bookingRepository, bookingSeatRepository, bookingService,
            bookingMapper, showtimeService, paymentService, ticketService, payOSService
        );

        User staff = User.builder().username("staff").build();
        staff.setId(2L);
        User walkin = User.builder().username("walkin").build();
        walkin.setId(3L);

        Movie movie = Movie.builder().title("Interstellar").status(MovieStatus.NOW_SHOWING).build();
        Showtime showtime = Showtime.builder()
            .movie(movie)
            .startTime(LocalDateTime.now().plusHours(2))
            .basePrice(new BigDecimal("90000"))
            .active(true)
            .build();
        showtime.setId(20L);

        Seat seat = Seat.builder().seatCode("C5").seatType("STANDARD").build();
        ShowtimeSeat ss = ShowtimeSeat.builder().showtime(showtime).seat(seat).status(SeatStatus.AVAILABLE).build();
        ss.setId(301L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(staff));
        when(userRepository.findByUsername("walkin")).thenReturn(Optional.of(walkin));
        when(showtimeRepository.findById(20L)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findAllByIdForUpdate(List.of(301L))).thenReturn(List.of(ss));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(800L);
            return b;
        });
        when(ticketService.issue(any(Booking.class))).thenReturn(
            Ticket.builder().ticketCode("TKT-POS800").qrCode("CINEX|TKT-POS800|BK800").status(TicketStatus.UNUSED).build()
        );
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(BookingResponse.builder().bookingCode("BK800").build());

        StaffSaleRequest req = new StaffSaleRequest();
        req.setShowtimeId(20L);
        req.setSeatIds(List.of(301L));
        req.setPaymentMethod("CASH");
        req.setSnackAmount(new BigDecimal("45000"));
        req.setCustomerName("Khách quầy");

        StaffSaleResponse saleResp = staffService.createSale(2L, req);
        assertThat(saleResp.getTicketCode()).isEqualTo("TKT-POS800");
        assertThat(ss.getStatus()).isEqualTo(SeatStatus.BOOKED);

        // Kiểm tra shiftSummary
        Booking counterBooking = Booking.builder()
            .bookingCode("BK800")
            .channel(BookingChannel.COUNTER)
            .status(BookingStatus.CONFIRMED)
            .totalAmount(new BigDecimal("135000"))
            .build();
        counterBooking.setId(800L);
        when(bookingRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(), any()))
            .thenReturn(List.of(counterBooking));
        when(paymentService.findByBookingId(800L)).thenReturn(Optional.of(
            Payment.builder().paymentMethod("CASH").amount(new BigDecimal("135000")).status(PaymentStatus.SUCCESS).build()
        ));
        when(bookingSeatRepository.findByBookingId(800L)).thenReturn(List.of(new BookingSeat()));

        ShiftSummaryResponse summary = staffService.shiftSummary(LocalDate.now());
        assertThat(summary.getTotalRevenue()).isEqualByComparingTo("135000");
        assertThat(summary.getCashRevenue()).isEqualByComparingTo("135000");
        assertThat(summary.getTicketCount()).isEqualTo(1);

        // Kiểm tra StaffService.validateTicket khi vé lỗi trả về valid=false mà không văng exception
        when(ticketService.validate("INVALID")).thenThrow(
            new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Vé không tồn tại")
        );
        TicketValidationResponse validation = staffService.validateTicket("INVALID");
        assertThat(validation.isValid()).isFalse();
        assertThat(validation.getMessage()).isEqualTo("Vé không tồn tại");
    }

    // ======================== 5. AUTH SERVICE ========================

    @Test
    void authServiceRegistersLogsInChangesPasswordAndResetsOtp() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        JwtService jwtService = mock(JwtService.class);
        AuthenticationManager authManager = mock(AuthenticationManager.class);
        EmailService emailService = mock(EmailService.class);

        AuthService authService = new AuthService(
            userRepository, roleRepository, passwordEncoder, jwtService, authManager, emailService
        );
        ReflectionTestUtils.setField(authService, "otpExpireMinutes", 5);
        ReflectionTestUtils.setField(authService, "exposeResetToken", true);

        Role customerRole = Role.builder().id(1L).name(ERole.ROLE_CUSTOMER).build();
        when(roleRepository.findByName(ERole.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token-xyz");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("newuser");
        reg.setEmail("newuser@cinex.vn");
        reg.setPassword("Secret@123");
        reg.setFullName("Người Dùng Mới");

        AuthResponse regResp = authService.register(reg);
        assertThat(regResp.getToken()).isEqualTo("jwt-token-xyz");
        assertThat(regResp.getRoles()).contains("ROLE_CUSTOMER");

        User savedUser = User.builder()
            .username("newuser")
            .email("newuser@cinex.vn")
            .password(passwordEncoder.encode("Secret@123"))
            .roles(Set.of(customerRole))
            .active(true)
            .build();
        savedUser.setId(55L);

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(savedUser));
        when(userRepository.findByEmail("newuser@cinex.vn")).thenReturn(Optional.of(savedUser));
        when(userRepository.findById(55L)).thenReturn(Optional.of(savedUser));

        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("newuser");
        loginReq.setPassword("Secret@123");
        AuthResponse loginResp = authService.login(loginReq);
        assertThat(loginResp.getUsername()).isEqualTo("newuser");

        // Đổi mật khẩu
        ChangePasswordRequest changePwd = new ChangePasswordRequest();
        changePwd.setCurrentPassword("Secret@123");
        changePwd.setNewPassword("NewSecret@456");
        authService.changePassword(savedUser, changePwd);
        assertThat(passwordEncoder.matches("NewSecret@456", savedUser.getPassword())).isTrue();

        // Quên mật khẩu & đặt lại mật khẩu bằng OTP
        PasswordResetResponse resetResp = authService.forgotPassword("newuser@cinex.vn");
        assertThat(resetResp.resetToken()).isNotBlank();
        when(userRepository.findByPasswordResetToken(savedUser.getPasswordResetToken()))
            .thenReturn(Optional.of(savedUser));

        authService.resetPassword(resetResp.resetToken(), "ResetPass@789");
        assertThat(passwordEncoder.matches("ResetPass@789", savedUser.getPassword())).isTrue();
        assertThat(savedUser.getPasswordResetToken()).isNull();
    }
}
