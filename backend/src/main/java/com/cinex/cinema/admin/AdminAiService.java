package com.cinex.cinema.admin;

import com.cinex.cinema.ai.AiChatMessage;
import com.cinex.cinema.ai.NvidiaAiService;
import com.cinex.cinema.cinema.Auditorium;
import com.cinex.cinema.cinema.AuditoriumRepository;
import com.cinex.cinema.cinema.AuditoriumRequest;
import com.cinex.cinema.cinema.AuditoriumResponse;
import com.cinex.cinema.cinema.AuditoriumService;
import com.cinex.cinema.cinema.Cinema;
import com.cinex.cinema.cinema.CinemaRepository;
import com.cinex.cinema.cinema.CinemaRequest;
import com.cinex.cinema.cinema.CinemaResponse;
import com.cinex.cinema.cinema.CinemaService;
import com.cinex.cinema.movie.Movie;
import com.cinex.cinema.movie.MovieRepository;
import com.cinex.cinema.movie.MovieRequest;
import com.cinex.cinema.movie.MovieResponse;
import com.cinex.cinema.movie.MovieService;
import com.cinex.cinema.movie.MovieStatus;
import com.cinex.cinema.showtime.Showtime;
import com.cinex.cinema.showtime.ShowtimeRequest;
import com.cinex.cinema.showtime.ShowtimeResponse;
import com.cinex.cinema.showtime.ShowtimeService;
import com.cinex.cinema.tmdb.TmdbClient;
import com.cinex.cinema.tmdb.TmdbDtos;
import com.cinex.cinema.trailer.YoutubeDtos;
import com.cinex.cinema.trailer.YoutubeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Trợ lý AI dành riêng cho Admin:
 * - Phân tích yêu cầu bằng ngôn ngữ tự nhiên (tạo phòng chiếu, thêm phim, tạo/cập nhật suất chiếu,
 *   hoặc tự động xếp lịch 10 ngày).
 * - Gọi trực tiếp các service nghiệp vụ để thực thi và trả về danh sách kết quả chi tiết.
 */
@Slf4j
@Service
public class AdminAiService {

    private static final ZoneId CINEMA_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile(
        "```(?:json)?\\s*(\\{[\\s\\S]*?})\\s*```",
        Pattern.CASE_INSENSITIVE
    );

    private static final String ADMIN_SYSTEM_PROMPT = """
        Bạn là **CineX Admin Copilot**, trợ lý AI quản trị hệ thống rạp chiếu phim CineX.
        Nhiệm vụ của bạn là hỗ trợ Admin thực hiện nhanh các thao tác:
        1. Tạo phòng chiếu mới (hệ thống sẽ tự động sinh sơ đồ ghế STANDARD/VIP/COUPLE).
        2. Thêm phim mới vào hệ thống (hệ thống tự động bổ sung poster/trailer từ TMDB & YouTube nếu có).
        3. Tạo suất chiếu mới (1 hoặc nhiều khung giờ) hoặc cập nhật suất chiếu.
        4. Tự động xếp/cập nhật lịch chiếu cho 10 ngày tới (chu kỳ tự động của hệ thống là 5 ngày/lần).
        5. Tạo cụm rạp mới nếu được yêu cầu.

        QUY TẮC QUAN TRỌNG KHI ADMIN YÊU CẦU THAO TÁC:
        - Hãy trả lời ngắn gọn, rõ ràng bằng tiếng Việt xác nhận bạn sẽ thực hiện thao tác gì.
        - Ở CUỐI câu trả lời, BẮT BUỘC xuất đúng MỘT khối ```json ... ``` theo mẫu dưới đây để hệ thống thực thi thật vào CSDL:

        ```json
        {
          "actions": [
            {
              "type": "CREATE_AUDITORIUM",
              "cinemaId": 1,
              "cinemaName": "Cinex Landmark 81",
              "name": "Screen 3",
              "totalRows": 10,
              "columnsPerRow": 12
            },
            {
              "type": "CREATE_MOVIE",
              "title": "Tên phim",
              "genre": "Hành động, Phiêu lưu",
              "durationMinutes": 125,
              "ageRating": 13,
              "status": "NOW_SHOWING",
              "description": "Mô tả ngắn gọn nội dung phim"
            },
            {
              "type": "CREATE_SHOWTIME",
              "movieId": 1,
              "movieTitle": "Dune: Part Two",
              "cinemaId": 1,
              "cinemaName": "Cinex Landmark 81",
              "auditoriumId": 1,
              "auditoriumName": "Screen 1",
              "startTimes": ["2026-11-11T18:00:00", "2026-11-11T21:30:00"],
              "basePrice": 90000
            },
            {
              "type": "UPDATE_SHOWTIME",
              "showtimeId": 10,
              "startTime": "2026-11-11T19:00:00",
              "basePrice": 100000
            },
            {
              "type": "AUTO_GENERATE_SHOWTIMES",
              "days": 10
            },
            {
              "type": "CREATE_CINEMA",
              "name": "Cinex Thảo Điền",
              "address": "Quận 2, TP.HCM",
              "phone": "1900 6020"
            }
          ]
        }
        ```

        LƯU Ý:
        - Chỉ đưa vào mảng "actions" những thao tác mà Admin thực sự yêu cầu trong tin nhắn hiện tại.
        - Nếu Admin chỉ hỏi thông tin hoặc hỏi cách dùng, trả lời bình thường và để `"actions": []` hoặc không cần khối json.
        - Tra cứu đúng `cinemaId`, `auditoriumId`, `movieId` từ phần DỮ LIỆU HỆ THỐNG bên dưới.
        - Với giá vé: "90k" = 90000, "100 nghìn" = 100000. Mặc định `basePrice` là 90000 nếu không nêu rõ.
        - Với kích thước phòng chiếu: mặc định `totalRows` = 8, `columnsPerRow` = 12 nếu Admin không nêu rõ.
        - Với thời lượng phim: mặc định `durationMinutes` = 120, `ageRating` = 13, `status` = "NOW_SHOWING" ("COMING_SOON" nếu nói "sắp chiếu").
        """;

    private final NvidiaAiService nvidiaAiService;
    private final CinemaRepository cinemaRepository;
    private final CinemaService cinemaService;
    private final AuditoriumRepository auditoriumRepository;
    private final AuditoriumService auditoriumService;
    private final MovieRepository movieRepository;
    private final MovieService movieService;
    private final ShowtimeService showtimeService;
    private final TmdbClient tmdbClient;
    private final YoutubeService youtubeService;
    private final ObjectMapper objectMapper;

    public AdminAiService(NvidiaAiService nvidiaAiService,
                          CinemaRepository cinemaRepository,
                          CinemaService cinemaService,
                          AuditoriumRepository auditoriumRepository,
                          AuditoriumService auditoriumService,
                          MovieRepository movieRepository,
                          MovieService movieService,
                          ShowtimeService showtimeService,
                          TmdbClient tmdbClient,
                          YoutubeService youtubeService,
                          ObjectMapper objectMapper) {
        this.nvidiaAiService = nvidiaAiService;
        this.cinemaRepository = cinemaRepository;
        this.cinemaService = cinemaService;
        this.auditoriumRepository = auditoriumRepository;
        this.auditoriumService = auditoriumService;
        this.movieRepository = movieRepository;
        this.movieService = movieService;
        this.showtimeService = showtimeService;
        this.tmdbClient = tmdbClient;
        this.youtubeService = youtubeService;
        this.objectMapper = objectMapper;
    }

    public AdminAiResponse chat(String message, List<AiChatMessage> history) {
        String userMessage = message == null ? "" : message.trim();
        String systemPrompt = ADMIN_SYSTEM_PROMPT + "\n\n" + buildAdminContext();

        String rawReply = null;
        String modelName = nvidiaAiService.getModel();

        try {
            if (nvidiaAiService.isConfigured()) {
                rawReply = nvidiaAiService.chat(systemPrompt, history, userMessage);
            }
        } catch (Exception ex) {
            log.warn("Gọi NVIDIA AI trong Admin gặp lỗi, chuyển sang bộ phân tích lệnh trực tiếp: {}", ex.getMessage());
        }

        List<JsonNode> actionNodes = new ArrayList<>();
        String cleanReply = "";

        if (rawReply != null && !rawReply.isBlank()) {
            actionNodes.addAll(extractJsonActions(rawReply));
            cleanReply = stripJsonBlocks(rawReply).trim();
        }

        List<AdminAiActionResult> executedActions = new ArrayList<>();
        if (!actionNodes.isEmpty()) {
            for (JsonNode node : actionNodes) {
                executedActions.addAll(executeActionNode(node));
            }
        } else {
            // Dự phòng khi AI không sinh JSON hoặc chưa cấu hình NVIDIA_API_KEY
            executedActions.addAll(tryExecuteFallbackCommand(userMessage));
        }

        boolean dataChanged = executedActions.stream().anyMatch(AdminAiActionResult::success);

        if (cleanReply.isBlank()) {
            if (!executedActions.isEmpty()) {
                cleanReply = buildSummaryFromActions(executedActions);
            } else {
                cleanReply = """
                    Mình là **Trợ lý AI Quản trị CineX**. Bạn có thể yêu cầu mình:
                    - **Tạo phòng chiếu:** *"Tạo phòng Screen 3 có 10 hàng 12 cột ở rạp Landmark 81"*
                    - **Thêm phim mới:** *"Thêm phim Deadpool & Wolverine đang chiếu, 128 phút, 16+"*
                    - **Tạo suất chiếu:** *"Thêm suất chiếu Dune: Part Two ngày mai lúc 18:00 và 21:00 ở Landmark 81 giá 95k"*
                    - **Tự động phủ lịch 10 ngày:** *"Tự động cập nhật suất chiếu cho 10 ngày tới"*
                    """.trim();
            }
        } else if (!executedActions.isEmpty()) {
            long failed = executedActions.stream().filter(a -> !a.success()).count();
            if (failed > 0 && executedActions.stream().noneMatch(AdminAiActionResult::success)) {
                cleanReply = buildSummaryFromActions(executedActions);
            }
        }

        return new AdminAiResponse(cleanReply, modelName, executedActions, dataChanged);
    }

    private String buildAdminContext() {
        LocalDateTime now = LocalDateTime.now(CINEMA_ZONE);
        LocalDate today = now.toLocalDate();
        StringBuilder sb = new StringBuilder(4096);

        sb.append("DỮ LIỆU HỆ THỐNG HIỆN TẠI:\n");
        sb.append("- Thời điểm hiện tại: ").append(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
            .append(" (Hôm nay: ").append(today).append(", Ngày mai: ").append(today.plusDays(1))
            .append(", Ngày kia: ").append(today.plusDays(2)).append(")\n");

        sb.append("\nDANH SÁCH RẠP & PHÒNG CHIẾU:\n");
        List<Cinema> cinemas = cinemaRepository.findAll();
        for (Cinema cinema : cinemas) {
            sb.append("- [cinemaId=").append(cinema.getId()).append("] ")
                .append(cinema.getName())
                .append(cinema.isActive() ? " (Đang hoạt động)" : " (Bảo trì)")
                .append(" | Phòng chiếu: ");
            List<Auditorium> auds = auditoriumRepository.findByCinemaId(cinema.getId());
            if (auds.isEmpty()) {
                sb.append("chưa có phòng");
            } else {
                for (int i = 0; i < auds.size(); i++) {
                    Auditorium a = auds.get(i);
                    if (i > 0) sb.append(", ");
                    sb.append("[auditoriumId=").append(a.getId()).append("] ")
                        .append(a.getName()).append(" (").append(a.getTotalRows()).append("x").append(a.getColumnsPerRow())
                        .append(a.isActive() ? "" : ", bảo trì").append(")");
                }
            }
            sb.append('\n');
        }

        sb.append("\nDANH SÁCH PHIM:\n");
        List<Movie> movies = movieRepository.findAll();
        for (Movie movie : movies) {
            sb.append("- [movieId=").append(movie.getId()).append("] ")
                .append(movie.getTitle())
                .append(" | ").append(movie.getDurationMinutes() == null ? 120 : movie.getDurationMinutes()).append(" phút")
                .append(" | ").append(movie.getStatus())
                .append(movie.isActive() ? "" : " (Đã ẩn)")
                .append('\n');
        }

        return sb.toString();
    }

    private List<JsonNode> extractJsonActions(String text) {
        List<JsonNode> result = new ArrayList<>();
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(text);
        while (matcher.find()) {
            parseActionsFromJsonString(matcher.group(1), result);
        }
        if (result.isEmpty()) {
            int firstBrace = text.indexOf('{');
            int lastBrace = text.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                parseActionsFromJsonString(text.substring(firstBrace, lastBrace + 1), result);
            }
        }
        return result;
    }

    private void parseActionsFromJsonString(String jsonCandidate, List<JsonNode> result) {
        try {
            JsonNode root = objectMapper.readTree(jsonCandidate);
            JsonNode actions = root.path("actions");
            if (actions.isArray()) {
                actions.forEach(result::add);
            } else if (root.has("type")) {
                result.add(root);
            }
        } catch (Exception ignored) {
            // Bỏ qua nếu đoạn JSON không hợp lệ
        }
    }

    private String stripJsonBlocks(String text) {
        String stripped = JSON_BLOCK_PATTERN.matcher(text).replaceAll("");
        return stripped.replaceAll("\\{\\s*\"actions\"\\s*:\\s*\\[[\\s\\S]*?]\\s*}", "").trim();
    }

    private List<AdminAiActionResult> executeActionNode(JsonNode node) {
        String type = node.path("type").asText("").trim().toUpperCase(Locale.ROOT);
        try {
            return switch (type) {
                case "CREATE_AUDITORIUM" -> List.of(handleCreateAuditorium(node));
                case "CREATE_CINEMA" -> List.of(handleCreateCinema(node));
                case "CREATE_MOVIE" -> List.of(handleCreateMovie(node));
                case "CREATE_SHOWTIME" -> handleCreateShowtime(node);
                case "UPDATE_SHOWTIME" -> List.of(handleUpdateShowtime(node));
                case "AUTO_GENERATE_SHOWTIMES", "AUTO_EXTEND_SHOWTIMES" -> List.of(handleAutoGenerateShowtimes(node));
                default -> List.of();
            };
        } catch (Exception ex) {
            String reason = extractErrorMessage(ex);
            return List.of(new AdminAiActionResult(type, false, "Không thể thực hiện " + type, reason, null));
        }
    }

    private AdminAiActionResult handleCreateCinema(JsonNode node) {
        String name = node.path("name").asText("").trim();
        if (name.isBlank()) {
            return new AdminAiActionResult("CREATE_CINEMA", false, "Thiếu tên rạp", "Vui lòng cung cấp tên rạp cần tạo", null);
        }
        CinemaRequest req = new CinemaRequest();
        req.setName(name);
        req.setAddress(node.path("address").asText("TP. Hồ Chí Minh"));
        req.setPhone(node.path("phone").asText("1900 6017"));
        CinemaResponse created = cinemaService.create(req);
        return new AdminAiActionResult(
            "CREATE_CINEMA",
            true,
            "Đã tạo rạp: " + created.getName(),
            "Mã rạp #" + created.getId() + " · Địa chỉ: " + created.getAddress(),
            created.getId()
        );
    }

    private AdminAiActionResult handleCreateAuditorium(JsonNode node) {
        Cinema cinema = resolveCinema(
            node.path("cinemaId").isNumber() ? node.path("cinemaId").asLong() : null,
            node.path("cinemaName").asText("")
        );
        if (cinema == null) {
            return new AdminAiActionResult(
                "CREATE_AUDITORIUM",
                false,
                "Không tìm thấy rạp chiếu",
                "Vui lòng nêu rõ tên rạp (ví dụ: Landmark 81, Galaxy Nguyễn Du, Aeon Tân Phú)",
                null
            );
        }

        String roomName = node.path("name").asText("").trim();
        if (roomName.isBlank()) {
            int nextIndex = auditoriumRepository.findByCinemaId(cinema.getId()).size() + 1;
            roomName = "Screen " + nextIndex;
        }
        int rows = node.path("totalRows").asInt(8);
        int cols = node.path("columnsPerRow").asInt(12);
        rows = Math.max(1, Math.min(30, rows));
        cols = Math.max(1, Math.min(30, cols));

        AuditoriumRequest req = new AuditoriumRequest();
        req.setName(roomName);
        req.setTotalRows(rows);
        req.setColumnsPerRow(cols);

        AuditoriumResponse created = auditoriumService.create(cinema.getId(), req);
        return new AdminAiActionResult(
            "CREATE_AUDITORIUM",
            true,
            "Đã tạo phòng chiếu: " + created.getName(),
            String.format("Rạp %s · %d hàng × %d cột (%d ghế tự động phân hạng)",
                created.getCinemaName(), created.getTotalRows(), created.getColumnsPerRow(), created.getSeatCount()),
            created.getId()
        );
    }

    private AdminAiActionResult handleCreateMovie(JsonNode node) {
        String title = node.path("title").asText("").trim();
        if (title.isBlank()) {
            return new AdminAiActionResult("CREATE_MOVIE", false, "Thiếu tên phim", "Vui lòng nhập tên phim cần thêm", null);
        }

        String description = node.path("description").asText("").trim();
        String posterUrl = node.path("posterUrl").asText("").trim();
        String trailerUrl = node.path("trailerUrl").asText("").trim();
        int duration = node.path("durationMinutes").asInt(0);
        int ageRating = node.path("ageRating").asInt(13);
        String genre = node.path("genre").asText("").trim();
        String statusStr = node.path("status").asText("NOW_SHOWING").trim().toUpperCase(Locale.ROOT);
        MovieStatus status = "COMING_SOON".equals(statusStr) ? MovieStatus.COMING_SOON : MovieStatus.NOW_SHOWING;

        // Tự động tra cứu TMDB để lấy poster, mô tả, thời lượng chuẩn nếu còn trống
        try {
            TmdbDtos.TmdbPage<TmdbDtos.TmdbMovie> searchPage = tmdbClient.search(title, 1);
            if (searchPage != null && searchPage.results() != null && !searchPage.results().isEmpty()) {
                TmdbDtos.TmdbMovie match = searchPage.results().get(0);
                if (posterUrl.isBlank() && match.posterPath() != null) {
                    posterUrl = tmdbClient.posterUrl(match.posterPath());
                }
                if (description.isBlank() && match.overview() != null && !match.overview().isBlank()) {
                    description = match.overview();
                }
                if (duration <= 0 && match.id() != null) {
                    TmdbDtos.TmdbMovie detail = tmdbClient.movieDetail(match.id());
                    if (detail != null && detail.runtime() != null && detail.runtime() > 0) {
                        duration = detail.runtime();
                    }
                }
            }
        } catch (Exception ignored) {
            // Không chặn việc tạo phim nếu TMDB chưa cấu hình
        }

        // Tự động tìm trailer YouTube nếu chưa có
        if (trailerUrl.isBlank()) {
            try {
                YoutubeDtos.YoutubeTrailer trailer = youtubeService.findFirstTrailer(title);
                if (trailer != null && trailer.embedUrl() != null) {
                    trailerUrl = trailer.embedUrl();
                }
            } catch (Exception ignored) {
                // Không chặn việc tạo phim nếu YouTube chưa cấu hình
            }
        }

        if (duration <= 0) {
            duration = 120;
        }
        if (genre.isBlank()) {
            genre = "Hành động, Giải trí";
        }
        if (description.isBlank()) {
            description = "Phim điện ảnh " + title + " trình chiếu tại hệ thống rạp CineX.";
        }

        MovieRequest req = new MovieRequest();
        req.setTitle(title);
        req.setDescription(description);
        req.setPosterUrl(posterUrl.isBlank() ? null : posterUrl);
        req.setTrailerUrl(trailerUrl.isBlank() ? null : trailerUrl);
        req.setDurationMinutes(duration);
        req.setAgeRating(ageRating);
        req.setGenre(genre);
        req.setReleaseDate(LocalDate.now(CINEMA_ZONE));
        req.setStatus(status);

        MovieResponse created = movieService.create(req);
        return new AdminAiActionResult(
            "CREATE_MOVIE",
            true,
            "Đã thêm phim: " + created.getTitle(),
            String.format("Mã phim #%d · %d phút · %s · %s",
                created.getId(),
                created.getDurationMinutes(),
                created.getGenre(),
                created.getStatus() == MovieStatus.NOW_SHOWING ? "Đang chiếu" : "Sắp chiếu"),
            created.getId()
        );
    }

    private List<AdminAiActionResult> handleCreateShowtime(JsonNode node) {
        Movie movie = resolveMovie(
            node.path("movieId").isNumber() ? node.path("movieId").asLong() : null,
            node.path("movieTitle").asText("")
        );
        if (movie == null) {
            return List.of(new AdminAiActionResult(
                "CREATE_SHOWTIME",
                false,
                "Không tìm thấy phim",
                "Vui lòng kiểm tra lại tên phim hoặc yêu cầu AI thêm phim trước khi xếp suất chiếu",
                null
            ));
        }

        Auditorium auditorium = resolveAuditorium(
            node.path("auditoriumId").isNumber() ? node.path("auditoriumId").asLong() : null,
            node.path("auditoriumName").asText(""),
            node.path("cinemaId").isNumber() ? node.path("cinemaId").asLong() : null,
            node.path("cinemaName").asText("")
        );
        if (auditorium == null) {
            return List.of(new AdminAiActionResult(
                "CREATE_SHOWTIME",
                false,
                "Không tìm thấy phòng chiếu phù hợp",
                "Vui lòng chỉ định rõ rạp hoặc phòng chiếu đang hoạt động",
                null
            ));
        }

        BigDecimal basePrice = new BigDecimal(node.path("basePrice").asLong(90000));
        if (basePrice.compareTo(BigDecimal.valueOf(1000)) < 0 && basePrice.signum() > 0) {
            // Phòng trường hợp AI trả 90 thay vì 90000
            basePrice = basePrice.multiply(BigDecimal.valueOf(1000));
        }

        List<LocalDateTime> startTimes = extractStartTimes(node);
        if (startTimes.isEmpty()) {
            startTimes = List.of(LocalDate.now(CINEMA_ZONE).plusDays(1).atTime(19, 30));
        }

        List<AdminAiActionResult> results = new ArrayList<>();
        for (LocalDateTime startTime : startTimes) {
            try {
                ShowtimeRequest req = new ShowtimeRequest();
                req.setMovieId(movie.getId());
                req.setAuditoriumId(auditorium.getId());
                req.setStartTime(startTime);
                req.setBasePrice(basePrice);

                ShowtimeResponse created = showtimeService.create(req);
                results.add(new AdminAiActionResult(
                    "CREATE_SHOWTIME",
                    true,
                    "Đã tạo suất chiếu: " + created.getMovieTitle(),
                    String.format("%s · %s (%s) · %s %s · %,dđ",
                        created.getCinemaName(),
                        created.getAuditoriumName(),
                        created.getTotalSeats() + " ghế",
                        created.getStartTime().format(DATE_FMT),
                        created.getStartTime().format(TIME_FMT),
                        created.getBasePrice().longValue()),
                    created.getId()
                ));
            } catch (Exception ex) {
                results.add(new AdminAiActionResult(
                    "CREATE_SHOWTIME",
                    false,
                    "Không tạo được suất " + startTime.format(DATE_FMT) + " " + startTime.format(TIME_FMT),
                    extractErrorMessage(ex),
                    null
                ));
            }
        }
        return results;
    }

    private AdminAiActionResult handleUpdateShowtime(JsonNode node) {
        if (!node.path("showtimeId").isNumber()) {
            return new AdminAiActionResult(
                "UPDATE_SHOWTIME",
                false,
                "Thiếu mã suất chiếu (#ID)",
                "Vui lòng nêu mã suất chiếu cần cập nhật (ví dụ: suất chiếu #12)",
                null
            );
        }
        long showtimeId = node.path("showtimeId").asLong();
        Showtime existing = showtimeService.findById(showtimeId);

        ShowtimeRequest req = new ShowtimeRequest();
        req.setMovieId(node.path("movieId").isNumber() ? node.path("movieId").asLong() : existing.getMovie().getId());
        req.setAuditoriumId(node.path("auditoriumId").isNumber()
            ? node.path("auditoriumId").asLong()
            : existing.getAuditorium().getId());

        List<LocalDateTime> parsedTimes = extractStartTimes(node);
        req.setStartTime(parsedTimes.isEmpty() ? existing.getStartTime() : parsedTimes.get(0));

        if (node.path("basePrice").isNumber()) {
            BigDecimal price = new BigDecimal(node.path("basePrice").asLong());
            if (price.compareTo(BigDecimal.valueOf(1000)) < 0 && price.signum() > 0) {
                price = price.multiply(BigDecimal.valueOf(1000));
            }
            req.setBasePrice(price);
        } else {
            req.setBasePrice(existing.getBasePrice());
        }

        ShowtimeResponse updated = showtimeService.update(showtimeId, req);
        return new AdminAiActionResult(
            "UPDATE_SHOWTIME",
            true,
            "Đã cập nhật suất chiếu #" + updated.getId(),
            String.format("%s · %s - %s · %s %s · %,dđ",
                updated.getMovieTitle(),
                updated.getCinemaName(),
                updated.getAuditoriumName(),
                updated.getStartTime().format(DATE_FMT),
                updated.getStartTime().format(TIME_FMT),
                updated.getBasePrice().longValue()),
            updated.getId()
        );
    }

    private AdminAiActionResult handleAutoGenerateShowtimes(JsonNode node) {
        int days = node.path("days").asInt(10);
        ShowtimeService.AutoScheduleResult res = showtimeService.autoGenerateShowtimes(days);
        return new AdminAiActionResult(
            "AUTO_GENERATE_SHOWTIMES",
            true,
            String.format("Đã tự động xếp lịch chiếu %d ngày (%s – %s)",
                res.daysAhead(), res.fromDate().format(DATE_FMT), res.toDate().format(DATE_FMT)),
            String.format("Tạo mới %d suất chiếu · Chuyển %d suất đã qua sang Hoàn thành (COMPLETED)",
                res.createdCount(), res.completedCount()),
            null
        );
    }

    /**
     * Bộ phân tích dự phòng bằng biểu thức chính quy tiếng Việt khi NVIDIA API không trả về JSON.
     */
    private List<AdminAiActionResult> tryExecuteFallbackCommand(String message) {
        if (message == null || message.isBlank()) {
            return List.of();
        }
        String lower = message.toLowerCase(Locale.ROOT);
        String norm = normalizeSearch(message);

        // 1. Tự động cập nhật / xếp lịch chiếu 10 ngày
        if ((norm.contains("tu dong") || norm.contains("cap nhat") || norm.contains("xep lich") || norm.contains("them lich"))
            && (norm.contains("10 ngay") || norm.contains("5 ngay") || norm.contains("suat chieu") && norm.contains("ngay toi"))) {
            int days = 10;
            Matcher m = Pattern.compile("(\\d{1,2})\\s*(?:ngay|ngày)", Pattern.CASE_INSENSITIVE).matcher(message);
            if (m.find()) {
                days = Integer.parseInt(m.group(1));
            }
            ShowtimeService.AutoScheduleResult res = showtimeService.autoGenerateShowtimes(days);
            return List.of(new AdminAiActionResult(
                "AUTO_GENERATE_SHOWTIMES",
                true,
                String.format("Đã tự động cập nhật suất chiếu cho %d ngày tới", res.daysAhead()),
                String.format("Từ %s đến %s · Thêm mới %d suất · Hoàn thành %d suất cũ",
                    res.fromDate().format(DATE_FMT), res.toDate().format(DATE_FMT),
                    res.createdCount(), res.completedCount()),
                null
            ));
        }

        // 2. Tạo phòng chiếu (ví dụ: "Tạo phòng Screen 3 10 hàng 12 cột ở rạp Landmark 81")
        if ((norm.contains("tao phong") || norm.contains("them phong")) && !norm.contains("suat chieu")) {
            Cinema cinema = resolveCinema(null, message);
            if (cinema == null) {
                cinema = cinemaRepository.findByActiveTrue().stream().findFirst().orElse(null);
            }
            if (cinema != null) {
                int rows = 8;
                int cols = 12;
                Matcher dimMatcher = Pattern.compile("(\\d{1,2})\\s*(?:x|×|hàng|hang)\\s*(\\d{1,2})", Pattern.CASE_INSENSITIVE).matcher(message);
                if (dimMatcher.find()) {
                    rows = Integer.parseInt(dimMatcher.group(1));
                    cols = Integer.parseInt(dimMatcher.group(2));
                }
                String roomName = extractRoomNameFromText(message, cinema.getId());
                AuditoriumRequest req = new AuditoriumRequest();
                req.setName(roomName);
                req.setTotalRows(Math.max(1, Math.min(30, rows)));
                req.setColumnsPerRow(Math.max(1, Math.min(30, cols)));
                AuditoriumResponse created = auditoriumService.create(cinema.getId(), req);
                return List.of(new AdminAiActionResult(
                    "CREATE_AUDITORIUM",
                    true,
                    "Đã tạo phòng chiếu: " + created.getName(),
                    String.format("Rạp %s · %d hàng × %d cột (%d ghế)",
                        created.getCinemaName(), created.getTotalRows(), created.getColumnsPerRow(), created.getSeatCount()),
                    created.getId()
                ));
            }
        }

        // 3. Thêm phim mới (ví dụ: "Thêm phim Lật Mặt 7 đang chiếu, 138 phút")
        if ((norm.contains("them phim") || norm.contains("tao phim"))
            && !norm.contains("suat chieu") && !norm.contains("lich chieu")) {
            Matcher titleMatcher = Pattern.compile(
                "(?i)(?:thêm|them|tạo|tao)\\s+phim\\s+(?:mới\\s+)?[\"']?([^,\"'\\n]+?)[\"']?(?:\\s+(?:đang chiếu|dang chieu|sắp chiếu|sap chieu|thể loại|the loai|\\d+\\s*phút|,|$))"
            ).matcher(message);
            String title = titleMatcher.find() ? titleMatcher.group(1).trim() : "";
            if (!title.isBlank()) {
                int duration = 120;
                Matcher durMatcher = Pattern.compile("(\\d{2,3})\\s*(?:phút|phut|p\\b)", Pattern.CASE_INSENSITIVE).matcher(message);
                if (durMatcher.find()) {
                    duration = Integer.parseInt(durMatcher.group(1));
                }
                MovieStatus status = norm.contains("sap chieu") ? MovieStatus.COMING_SOON : MovieStatus.NOW_SHOWING;
                ObjectMapper mapper = this.objectMapper;
                JsonNode node = mapper.createObjectNode()
                    .put("type", "CREATE_MOVIE")
                    .put("title", title)
                    .put("durationMinutes", duration)
                    .put("status", status.name());
                return List.of(handleCreateMovie(node));
            }
        }

        // 4. Tạo suất chiếu (ví dụ: "Tạo suất chiếu Dune ngày mai lúc 18:00 ở Landmark 81 giá 95k")
        if (norm.contains("suat chieu") || norm.contains("lich chieu")) {
            Movie movie = resolveMovie(null, message);
            Auditorium auditorium = resolveAuditorium(null, message, null, message);
            if (movie != null && auditorium != null) {
                LocalDate targetDate = LocalDate.now(CINEMA_ZONE);
                if (norm.contains("ngay mai") || norm.contains("mai")) {
                    targetDate = targetDate.plusDays(1);
                } else if (norm.contains("ngay kia") || norm.contains("mot")) {
                    targetDate = targetDate.plusDays(2);
                }
                List<LocalTime> times = new ArrayList<>();
                Matcher timeMatcher = Pattern.compile("(?<!\\d)([01]?\\d|2[0-3])[:h]([0-5]\\d)(?!\\d)").matcher(lower);
                while (timeMatcher.find()) {
                    times.add(LocalTime.of(Integer.parseInt(timeMatcher.group(1)), Integer.parseInt(timeMatcher.group(2))));
                }
                if (times.isEmpty()) {
                    times.add(LocalTime.of(19, 30));
                }
                BigDecimal price = new BigDecimal("90000");
                Matcher priceMatcher = Pattern.compile("(\\d{2,3})\\s*k\\b|(\\d{5,6})\\s*(?:đ|d|vnd)?", Pattern.CASE_INSENSITIVE).matcher(lower);
                if (priceMatcher.find()) {
                    if (priceMatcher.group(1) != null) {
                        price = BigDecimal.valueOf(Long.parseLong(priceMatcher.group(1)) * 1000L);
                    } else if (priceMatcher.group(2) != null) {
                        price = BigDecimal.valueOf(Long.parseLong(priceMatcher.group(2)));
                    }
                }
                List<AdminAiActionResult> list = new ArrayList<>();
                for (LocalTime t : times) {
                    try {
                        ShowtimeRequest req = new ShowtimeRequest();
                        req.setMovieId(movie.getId());
                        req.setAuditoriumId(auditorium.getId());
                        req.setStartTime(targetDate.atTime(t));
                        req.setBasePrice(price);
                        ShowtimeResponse created = showtimeService.create(req);
                        list.add(new AdminAiActionResult(
                            "CREATE_SHOWTIME",
                            true,
                            "Đã tạo suất chiếu: " + created.getMovieTitle(),
                            String.format("%s · %s · %s %s · %,dđ",
                                created.getCinemaName(),
                                created.getAuditoriumName(),
                                created.getStartTime().format(DATE_FMT),
                                created.getStartTime().format(TIME_FMT),
                                created.getBasePrice().longValue()),
                            created.getId()
                        ));
                    } catch (Exception ex) {
                        list.add(new AdminAiActionResult(
                            "CREATE_SHOWTIME",
                            false,
                            "Không tạo được suất lúc " + t.format(TIME_FMT),
                            extractErrorMessage(ex),
                            null
                        ));
                    }
                }
                return list;
            }
        }

        return List.of();
    }

    private String extractRoomNameFromText(String text, Long cinemaId) {
        Matcher m = Pattern.compile("(?i)(?:phòng|phong|screen)\\s+([A-Za-z0-9_\\- ]{1,20}?)(?=\\s+(?:có|co|\\d|tại|tai|ở|o|rạp|rap|$))").matcher(text);
        if (m.find()) {
            String candidate = m.group(1).trim();
            if (text.toLowerCase(Locale.ROOT).contains("screen " + candidate.toLowerCase(Locale.ROOT))) {
                return "Screen " + candidate;
            }
            return candidate.matches("\\d+") ? "Screen " + candidate : candidate;
        }
        int next = auditoriumRepository.findByCinemaId(cinemaId).size() + 1;
        return "Screen " + next;
    }

    private List<LocalDateTime> extractStartTimes(JsonNode node) {
        List<LocalDateTime> times = new ArrayList<>();
        if (node.path("startTimes").isArray()) {
            for (JsonNode item : node.path("startTimes")) {
                LocalDateTime dt = parseDateTimeFlexible(item.asText(""));
                if (dt != null) times.add(dt);
            }
        }
        if (times.isEmpty() && !node.path("startTime").asText("").isBlank()) {
            LocalDateTime dt = parseDateTimeFlexible(node.path("startTime").asText(""));
            if (dt != null) times.add(dt);
        }
        if (times.isEmpty() && !node.path("date").asText("").isBlank() && node.path("times").isArray()) {
            String dateStr = node.path("date").asText("").trim();
            for (JsonNode t : node.path("times")) {
                LocalDateTime dt = parseDateTimeFlexible(dateStr + "T" + t.asText("").trim());
                if (dt != null) times.add(dt);
            }
        }
        return times;
    }

    private LocalDateTime parseDateTimeFlexible(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String clean = raw.trim().replace(' ', 'T');
        try {
            if (clean.length() == 16) {
                clean = clean + ":00";
            }
            return LocalDateTime.parse(clean);
        } catch (Exception ex) {
            return null;
        }
    }

    private Cinema resolveCinema(Long cinemaId, String cinemaQuery) {
        if (cinemaId != null) {
            Cinema byId = cinemaRepository.findById(cinemaId).orElse(null);
            if (byId != null) return byId;
        }
        String normQuery = normalizeSearch(cinemaQuery);
        List<Cinema> cinemas = cinemaRepository.findAll();
        if (!normQuery.isBlank()) {
            for (Cinema c : cinemas) {
                String normName = normalizeSearch(c.getName());
                if (normName.contains(normQuery) || normQuery.contains(normName)) {
                    return c;
                }
                for (String token : List.of("landmark", "galaxy", "nguyen du", "aeon", "tan phu")) {
                    if (normQuery.contains(token) && normName.contains(token)) {
                        return c;
                    }
                }
            }
        }
        return cinemas.stream().filter(Cinema::isActive).findFirst().orElse(null);
    }

    private Auditorium resolveAuditorium(Long auditoriumId, String auditoriumQuery,
                                         Long cinemaId, String cinemaQuery) {
        if (auditoriumId != null) {
            Auditorium byId = auditoriumRepository.findById(auditoriumId).orElse(null);
            if (byId != null && byId.isActive()) return byId;
        }
        Cinema cinema = resolveCinema(cinemaId, cinemaQuery != null && !cinemaQuery.isBlank() ? cinemaQuery : auditoriumQuery);
        List<Auditorium> candidates = cinema != null
            ? auditoriumRepository.findByCinemaIdAndActiveTrue(cinema.getId())
            : auditoriumRepository.findAll().stream().filter(Auditorium::isActive).toList();

        String normAud = normalizeSearch(auditoriumQuery);
        if (!normAud.isBlank()) {
            for (Auditorium a : candidates) {
                String normName = normalizeSearch(a.getName());
                if (normName.equals(normAud) || normAud.contains(normName) || normName.contains(normAud)) {
                    return a;
                }
            }
        }
        return candidates.stream()
            .min(Comparator.comparing(Auditorium::getId))
            .orElse(null);
    }

    private Movie resolveMovie(Long movieId, String movieQuery) {
        if (movieId != null) {
            Movie byId = movieRepository.findById(movieId).orElse(null);
            if (byId != null) return byId;
        }
        String normQuery = normalizeSearch(movieQuery);
        if (normQuery.isBlank()) return null;

        List<Movie> movies = movieRepository.findAll();
        for (Movie m : movies) {
            String normTitle = normalizeSearch(m.getTitle());
            if (!normTitle.isBlank() && (normTitle.contains(normQuery) || normQuery.contains(normTitle))) {
                return m;
            }
        }
        // Khớp theo từ khoá chính của tên phim (ví dụ "Dune", "Batman", "Spider-Man")
        for (Movie m : movies) {
            String[] words = normalizeSearch(m.getTitle()).split("\\s+");
            for (String w : words) {
                if (w.length() >= 4 && normQuery.contains(w)) {
                    return m;
                }
            }
        }
        return null;
    }

    private String buildSummaryFromActions(List<AdminAiActionResult> actions) {
        StringBuilder sb = new StringBuilder("Mình đã xử lý yêu cầu của bạn:\n");
        for (AdminAiActionResult action : actions) {
            sb.append(action.success() ? "- ✅ **" : "- ⚠️ **")
                .append(action.title()).append("**: ")
                .append(action.detail()).append('\n');
        }
        return sb.toString().trim();
    }

    private static String normalizeSearch(String input) {
        if (input == null) return "";
        String nfd = Normalizer.normalize(input, Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{M}+", "")
            .replace('đ', 'd')
            .replace('Đ', 'd')
            .toLowerCase(Locale.ROOT)
            .trim();
    }

    private static String extractErrorMessage(Exception ex) {
        if (ex instanceof org.springframework.web.server.ResponseStatusException rse && rse.getReason() != null) {
            return rse.getReason();
        }
        return ex.getMessage() != null ? ex.getMessage() : "Có lỗi xảy ra khi thực thi";
    }
}
