package com.cinex.cinema.ai;

import com.cinex.cinema.cinema.Auditorium;
import com.cinex.cinema.cinema.Cinema;
import com.cinex.cinema.movie.Movie;
import com.cinex.cinema.movie.MovieRepository;
import com.cinex.cinema.showtime.Showtime;
import com.cinex.cinema.showtime.ShowtimeRepository;
import com.cinex.cinema.showtime.ShowtimeStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Khoá hành vi chống bịa suất chiếu: câu trả lời của AI phải khớp dữ liệu thật trong DB.
 */
class CinemaAiContextServiceTest {

    private static final ZoneId CINEMA_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final MovieRepository movieRepository = mock(MovieRepository.class);
    private final ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
    private final CinemaAiContextService service =
        new CinemaAiContextService(movieRepository, showtimeRepository);

    @Test
    void keepsReplyWhenItDoesNotMentionAnyClockTime() {
        givenUpcomingShowtimes(showtime("20:30", "20:30", "Phim A"));

        String reply = "CineX hiện có suất chiếu cho phim A, bạn xem chi tiết ở trang Lịch chiếu nhé.";

        assertThat(service.sanitizeReply("Suất chiếu hôm nay thế nào?", reply)).isEqualTo(reply);
    }

    @Test
    void replacesFabricatedShowtimesWithEmptyNoticeWhenDatabaseHasNoShowtime() {
        givenUpcomingShowtimes();

        String reply = "Hôm nay CineX có suất chiếu lúc 19:00 phim A và 21:15 phim B.";

        assertThat(service.sanitizeReply("Suất chiếu hôm nay?", reply))
            .contains("chưa có suất chiếu nào trong 7 ngày tới")
            .doesNotContain("19:00")
            .doesNotContain("21:15");
    }

    @Test
    void replacesFabricatedClockTimeWithRealShowtimes() {
        givenUpcomingShowtimes(showtime("20:30", "22:45", "Phim A"));

        String reply = "CineX có suất lúc 19:00 phim A, bạn đặt vé ngay nhé.";

        String sanitized = service.sanitizeReply("Suất chiếu tối nay?", reply);

        assertThat(sanitized)
            .contains("20:30")
            .contains("Phim A")
            .contains("rạp CineX Quận 1")
            .doesNotContain("19:00");
    }

    @Test
    void keepsReplyWhenMentionedTimesExistInDatabase() {
        givenUpcomingShowtimes(showtime("20:30", "22:45", "Phim A"));

        String reply = "Suất chiếu còn bán vé là 20:30, dự kiến kết thúc 22:45.";

        assertThat(service.sanitizeReply("Suất chiếu tối nay?", reply)).isEqualTo(reply);
    }

    @Test
    void keepsUnrelatedReplyWithClockTime() {
        givenUpcomingShowtimes();

        String reply = "CineX mở cửa lúc 8:00 mỗi ngày, bạn ghé sớm nhé.";

        assertThat(service.sanitizeReply("Rạp mở cửa lúc mấy giờ?", reply)).isEqualTo(reply);
    }

    private void givenUpcomingShowtimes(Showtime... showtimes) {
        when(showtimeRepository.findByStartTimeBetweenAndActiveTrueOrderByStartTimeAsc(any(), any()))
            .thenReturn(List.of(showtimes));
    }

    private static Showtime showtime(String startTime, String endTime, String movieTitle) {
        LocalDate date = LocalDate.now(CINEMA_ZONE).plusDays(1);
        Cinema cinema = Cinema.builder().name("CineX Quận 1").address("1 Nguyễn Huệ").build();
        Auditorium auditorium = Auditorium.builder().name("Phòng 1").cinema(cinema).build();
        Movie movie = Movie.builder().title(movieTitle).build();

        return Showtime.builder()
            .movie(movie)
            .auditorium(auditorium)
            .startTime(date.atTime(LocalTime.parse(startTime)))
            .endTime(date.atTime(LocalTime.parse(endTime)))
            .basePrice(BigDecimal.valueOf(90_000))
            .status(ShowtimeStatus.ACTIVE)
            .active(true)
            .build();
    }
}
