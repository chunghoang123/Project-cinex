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

/**
 * Dựng dữ liệu thật của CineX (phim đang chiếu + suất chiếu từ thời điểm hiện tại)
 * để mẫu AI chỉ nhắc suất còn bán vé, giống trang Lịch chiếu.
 */
@Service
public class CinemaAiContextService {

    private static final ZoneId CINEMA_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final NumberFormat PRICE_FORMAT =
        NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
    private static final int MAX_SHOWTIMES = 150;
    private static final int MAX_DESCRIPTION_LENGTH = 240;

    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;

    public CinemaAiContextService(MovieRepository movieRepository,
                                  ShowtimeRepository showtimeRepository) {
        this.movieRepository = movieRepository;
        this.showtimeRepository = showtimeRepository;
    }

    @Transactional(readOnly = true)
    public String buildContext() {
        LocalDateTime now = LocalDateTime.now(CINEMA_ZONE);
        List<Movie> movies = movieRepository.findByStatusAndActiveTrue(MovieStatus.NOW_SHOWING);
        List<Showtime> showtimes = showtimeRepository
            .findByStartTimeBetweenAndActiveTrueOrderByStartTimeAsc(
                now,
                now.plusDays(7)
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
                builder.append("- [id=").append(movie.getId()).append("] ")
                    .append(movie.getTitle());
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
            builder.append("- Không còn suất chiếu nào trong 7 ngày tới.\n");
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

    /**
     * Tìm các phim thật được nhắc tới trong câu trả lời của AI để frontend hiển thị poster.
     * Chỉ trả về tối đa 3 phim, theo thứ tự lần đầu xuất hiện trong câu trả lời.
     */
    @Transactional(readOnly = true)
    public List<AiMovieSuggestion> findReferencedMovies(String reply) {
        if (reply == null || reply.isBlank()) {
            return List.of();
        }
        String lower = reply.toLowerCase(Locale.ROOT);
        List<Movie> movies = movieRepository.findByStatusAndActiveTrue(MovieStatus.NOW_SHOWING);
        return movies.stream()
            .filter(movie -> movie.getTitle() != null
                && !movie.getTitle().isBlank()
                && lower.contains(movie.getTitle().toLowerCase(Locale.ROOT)))
            .map(movie -> new AiMovieSuggestion(
                movie.getId(),
                movie.getTitle(),
                movie.getPosterUrl(),
                movie.getGenre()
            ))
            .limit(3)
            .toList();
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
