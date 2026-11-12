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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final int MAX_FALLBACK_SHOWTIMES = 8;
    private static final int MAX_DESCRIPTION_LENGTH = 240;

    /** Giờ dạng 19:00, 9:05 hay 19h00. */
    private static final Pattern CLOCK_PATTERN = Pattern.compile(
        "(?<!\\d)([01]?\\d|2[0-3])[:h]([0-5]\\d)(?!\\d)");
    /**
     * Chỉ can thiệp khi câu hỏi hoặc câu trả lời đang nói về suất chiếu,
     * tránh thay thế câu trả lời không liên quan (ví dụ giờ mở cửa).
     */
    private static final List<String> SHOWTIME_TOPIC_KEYWORDS = List.of(
        "suất chiếu", "suat chieu",
        "giờ chiếu", "gio chieu",
        "lịch chiếu", "lich chieu",
        "buổi chiếu", "buoi chieu",
        "chiếu lúc", "chieu luc"
    );

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
        LocalDateTime now = LocalDateTime.now(CINEMA_ZONE);
        List<Movie> movies = movieRepository.findByStatusAndActiveTrue(MovieStatus.NOW_SHOWING);
        List<Showtime> showtimes = loadUpcomingShowtimes(now);

        StringBuilder builder = new StringBuilder(4096);
        builder.append("Bây giờ là ").append(now.format(TIME_FORMAT))
            .append(' ').append(now.format(DATE_FORMAT))
            .append(" (giờ Việt Nam). Chỉ các suất TRONG DANH SÁCH dưới đây mới còn bán vé.\n\n");

        appendMoviesSection(builder, movies);
        appendShowtimesSection(builder, showtimes);

        builder.append("""
 
            QUY ĐỊNH CINEX:
            - Thanh toán trực tuyến chỉ qua PayOS; khách không tự huỷ vé sau khi đã thanh toán.
            - Ghế chỉ được giữ sau khi thanh toán thành công.
            - Khách soát vé bằng mã QR tại rạp.
            """);

        return builder.toString();
    }

    private void appendMoviesSection(StringBuilder builder, List<Movie> movies) {
        builder.append("PHIM ĐANG CHIẾU TẠI CINEX:\n");
        if (movies.isEmpty()) {
            builder.append("- Hiện chưa có phim nào đang chiếu trong hệ thống.\n");
            return;
        }
        for (Movie movie : movies) {
            appendMovieLine(builder, movie);
        }
    }

    private void appendMovieLine(StringBuilder builder, Movie movie) {
        builder.append("- [id=").append(movie.getId()).append("] ")
            .append(movie.getTitle());
        appendOptionalField(builder, " | thể loại: ", movie.getGenre());
        if (movie.getDurationMinutes() != null) {
            builder.append(" | ").append(movie.getDurationMinutes()).append(" phút");
        }
        if (movie.getAgeRating() != null) {
            builder.append(" | giới hạn ").append(movie.getAgeRating()).append("+");
        }
        appendOptionalField(builder, "\n  poster: ", movie.getPosterUrl());
        if (movie.getDescription() != null && !movie.getDescription().isBlank()) {
            builder.append("\n  Nội dung: ").append(shorten(movie.getDescription()));
        }
        builder.append('\n');
    }

    private static void appendOptionalField(StringBuilder builder, String prefix, String value) {
        if (value != null && !value.isBlank()) {
            builder.append(prefix).append(value);
        }
    }

    private void appendShowtimesSection(StringBuilder builder, List<Showtime> showtimes) {
        builder.append("\nSUẤT CHIẾU CÒN BÁN VÉ (từ bây giờ đến 7 ngày tới):\n");
        if (showtimes.isEmpty()) {
            builder.append("- Không còn suất chiếu nào trong 7 ngày tới.\n");
            return;
        }
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

    /**
     * Chặn câu trả lời bịa suất chiếu bằng cách đối chiếu giờ được nhắc với dữ liệu thật.
     * <ul>
     *   <li>DB không có suất nào trong 7 ngày tới mà câu trả lời vẫn nêu giờ chiếu → trả lời chuẩn "chưa có suất".</li>
     *   <li>DB có suất nhưng câu trả lời nêu giờ không tồn tại → thay bằng danh sách suất thật.</li>
     * </ul>
     * Câu trả lời không liên quan tới suất chiếu được giữ nguyên.
     */
    @Transactional(readOnly = true)
    public String sanitizeReply(String userMessage, String reply) {
        if (reply == null || reply.isBlank()) {
            return reply;
        }
        Set<String> mentionedTimes = extractTimes(reply);
        if (mentionedTimes.isEmpty()) {
            return reply;
        }
        if (!matchesShowtimeTopic(reply) && !matchesShowtimeTopic(userMessage)) {
            return reply;
        }

        List<Showtime> upcoming = loadUpcomingShowtimes(LocalDateTime.now(CINEMA_ZONE));
        if (upcoming.isEmpty()) {
            return EMPTY_SHOWTIME_REPLY;
        }

        Set<String> realTimes = new LinkedHashSet<>();
        for (Showtime showtime : upcoming) {
            realTimes.add(showtime.getStartTime().format(TIME_FORMAT));
            // Cho phép câu trả lời nhắc giờ kết thúc của suất thật.
            realTimes.add(showtime.getEndTime().format(TIME_FORMAT));
        }
        return realTimes.containsAll(mentionedTimes) ? reply : buildShowtimeFallback(upcoming);
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

    private List<Showtime> loadUpcomingShowtimes(LocalDateTime now) {
        return showtimeRepository.findByStartTimeBetweenAndActiveTrueOrderByStartTimeAsc(
            now,
            now.plusDays(7)
        );
    }

    private static Set<String> extractTimes(String text) {
        Set<String> times = new LinkedHashSet<>();
        Matcher matcher = CLOCK_PATTERN.matcher(text);
        while (matcher.find()) {
            times.add(String.format("%02d:%02d",
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2))));
        }
        return times;
    }

    private static boolean matchesShowtimeTopic(String text) {
        return text != null && !text.isBlank() && SHOWTIME_TOPIC_PATTERN.matcher(text).find();
    }

    private static String buildShowtimeFallback(List<Showtime> upcoming) {
        StringBuilder builder = new StringBuilder(
            "Mình kiểm tra lại dữ liệu thật của CineX thì hiện chỉ có các suất chiếu sau là còn bán vé:\n\n");
        upcoming.stream().limit(MAX_FALLBACK_SHOWTIMES).forEach(showtime -> builder
            .append("- **").append(showtime.getStartTime().format(DATE_FORMAT))
            .append("** lúc **").append(showtime.getStartTime().format(TIME_FORMAT)).append("** · ")
            .append(showtime.getMovie().getTitle())
            .append(" · rạp ").append(showtime.getAuditorium().getCinema().getName())
            .append(" - phòng ").append(showtime.getAuditorium().getName())
            .append(" · ").append(formatPrice(showtime.getBasePrice()))
            .append('\n'));
        if (upcoming.size() > MAX_FALLBACK_SHOWTIMES) {
            builder.append("- (Còn ").append(upcoming.size() - MAX_FALLBACK_SHOWTIMES)
                .append(" suất khác, bạn xem đầy đủ ở trang Suất chiếu.)\n");
        }
        builder.append("\nBạn xem đầy đủ ở trang **Lịch chiếu** và chọn suất phù hợp nhé.");
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
