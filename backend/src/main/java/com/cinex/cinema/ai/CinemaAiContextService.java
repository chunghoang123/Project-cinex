package com.cinex.cinema.ai;

import com.cinex.cinema.movie.Movie;
import com.cinex.cinema.movie.MovieRepository;
import com.cinex.cinema.movie.MovieStatus;
import com.cinex.cinema.showtime.Showtime;
import com.cinex.cinema.showtime.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dựng dữ liệu thật của CineX (phim đang chiếu + suất chiếu 7 ngày tới)
 * để đưa vào prompt, tránh việc AI bịa phim, rạp, giờ chiếu hay giá vé.
 */
@Service
public class CinemaAiContextService {

    private static final ZoneId CINEMA_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final NumberFormat PRICE_FORMAT =
        NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
    private static final int MAX_SHOWTIMES = 150;
    private static final int MAX_FALLBACK_SHOWTIMES = 8;
    private static final int MAX_DESCRIPTION_LENGTH = 240;

    /** Giờ dạng 19:00, 9:05 hay 19h00. */
    private static final Pattern CLOCK_PATTERN = Pattern.compile(
        "(?<!\\d)([01]?\\d|2[0-3])[:h]([0-5]\\d)(?!\\d)");
    /**
     * Chỉ can thiệp khi câu hỏi hoặc câu trả lời đang nói về suất chiếu,
     * tránh thay thế câu trả lời không liên quan (ví dụ giờ mở cửa).
     */
    private static final Pattern SHOWTIME_TOPIC_PATTERN = Pattern.compile(
        "(?i)(suất chiếu|suat chieu|giờ chiếu|gio chieu|lịch chiếu|lich chieu|"
        + "buổi chiếu|buoi chieu|chiếu lúc|chieu luc)",
        Pattern.UNICODE_CHARACTER_CLASS);

    private static final String EMPTY_SHOWTIME_REPLY = """
        Hiện CineX chưa có suất chiếu nào trong 7 ngày tới nên mình chưa thể gợi ý giờ chiếu cụ thể.

        Bạn vui lòng xem trang **Lịch chiếu** sau khi rạp cập nhật suất mới nhé.
        """.strip();

    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;

    public CinemaAiContextService(MovieRepository movieRepository,
                                  ShowtimeRepository showtimeRepository) {
        this.movieRepository = movieRepository;
        this.showtimeRepository = showtimeRepository;
    }

    @Transactional(readOnly = true)
    public String buildContext() {
        LocalDate today = LocalDate.now(CINEMA_ZONE);
        List<Movie> movies = movieRepository.findByStatusAndActiveTrue(MovieStatus.NOW_SHOWING);
        List<Showtime> showtimes = showtimeRepository
            .findByStartTimeBetweenAndActiveTrueOrderByStartTimeAsc(
                today.atStartOfDay(),
                today.plusDays(7).atStartOfDay()
            );

        StringBuilder builder = new StringBuilder(4096);
        builder.append("Bây giờ là ").append(now.format(TIME_FORMAT))
            .append(' ').append(now.format(DATE_FORMAT))
            .append(" (giờ Việt Nam). Chỉ các suất TRONG DANH SÁCH dưới đây mới còn bán vé.\n\n");

        builder.append("PHIM ĐANG CHIẾU TẠI CINEX:\n");
        if (movies.isEmpty()) {
            builder.append("- Hiện chưa có phim nào đang chiếu trong hệ thống.\n");
        } else {
            for (Movie movie : movies) {
                builder.append("- ").append(movie.getTitle());
                if (movie.getGenre() != null && !movie.getGenre().isBlank()) {
                    builder.append(" | thể loại: ").append(movie.getGenre());
                }
                if (movie.getDurationMinutes() != null) {
                    builder.append(" | ").append(movie.getDurationMinutes()).append(" phút");
                }
                if (movie.getAgeRating() != null) {
                    builder.append(" | giới hạn ").append(movie.getAgeRating()).append("+");
                }
                if (movie.getPosterUrl() != null && !movie.getPosterUrl().isBlank()) {
                    builder.append("\n  poster: ").append(movie.getPosterUrl());
                }
                if (movie.getDescription() != null && !movie.getDescription().isBlank()) {
                    builder.append("\n  Nội dung: ").append(shorten(movie.getDescription()));
                }
                builder.append('\n');
            }
        }

        builder.append("\nSUẤT CHIẾU CÒN BÁN VÉ (từ bây giờ đến 7 ngày tới):\n");
        if (showtimes.isEmpty()) {
            builder.append("- Chưa có suất chiếu nào trong 7 ngày tới.\n");
        } else {
            showtimes.stream().limit(MAX_SHOWTIMES).forEach(showtime ->
                builder.append("- ")
                    .append(showtime.getStartTime().format(DATE_FORMAT))
                    .append(' ').append(showtime.getStartTime().format(TIME_FORMAT))
                    .append(" | ").append(showtime.getMovie().getTitle())
                    .append(" | rạp ").append(showtime.getAuditorium().getCinema().getName())
                    .append(" | phòng ").append(showtime.getAuditorium().getName())
                    .append(" | giá ").append(formatPrice(showtime.getBasePrice()))
                    .append('\n')
            );
            if (showtimes.size() > MAX_SHOWTIMES) {
                builder.append("- (Còn ").append(showtimes.size() - MAX_SHOWTIMES)
                    .append(" suất khác, khách xem đầy đủ ở trang Suất chiếu.)\n");
            }
        }

        builder.append("""
 
            QUY ĐỊNH CINEX:
            - Thanh toán trực tuyến chỉ qua PayOS; khách không tự huỷ vé sau khi đã thanh toán.
            - Ghế chỉ được giữ sau khi thanh toán thành công.
            - Khách soát vé bằng mã QR tại rạp.
            """);

        return builder.toString();
    }

    private static String shorten(String value) {
        String trimmed = value.trim().replaceAll("\\s+", " ");
        return trimmed.length() <= MAX_DESCRIPTION_LENGTH
            ? trimmed
            : trimmed.substring(0, MAX_DESCRIPTION_LENGTH) + "...";
    }

    private static String formatPrice(BigDecimal price) {
        return price == null ? "đang cập nhật" : PRICE_FORMAT.format(price) + "đ";
    }
}
