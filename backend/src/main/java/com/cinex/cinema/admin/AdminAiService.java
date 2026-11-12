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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private static final DateTimeFormatter NOW_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String ACTION_CREATE_AUDITORIUM = "CREATE_AUDITORIUM";
    private static final String ACTION_CREATE_CINEMA = "CREATE_CINEMA";
    private static final String ACTION_CREATE_MOVIE = "CREATE_MOVIE";
    private static final String ACTION_CREATE_SHOWTIME = "CREATE_SHOWTIME";
    private static final String ACTION_UPDATE_SHOWTIME = "UPDATE_SHOWTIME";
    private static final String ACTION_AUTO_GENERATE = "AUTO_GENERATE_SHOWTIMES";

    private static final String FIELD_TYPE = "type";
    private static final String FIELD_ACTIONS = "actions";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_DURATION_MINUTES = "durationMinutes";
    private static final String FIELD_CINEMA_ID = "cinemaId";
    private static final String FIELD_CINEMA_NAME = "cinemaName";
    private static final String FIELD_AUDITORIUM_ID = "auditoriumId";
    private static final String FIELD_AUDITORIUM_NAME = "auditoriumName";
    private static final String FIELD_MOVIE_ID = "movieId";
    private static final String FIELD_SHOWTIME_ID = "showtimeId";
    private static final String FIELD_BASE_PRICE = "basePrice";
    private static final String FIELD_START_TIMES = "startTimes";
    private static final String FIELD_START_TIME = "startTime";
    private static final String FIELD_TIMES = "times";

    private static final String SCREEN_PREFIX = "Screen ";
    private static final String STATUS_NOW_SHOWING = "NOW_SHOWING";
    private static final String TOKEN_SUAT_CHIEU = "suat chieu";
    private static final String TOKEN_LICH_CHIEU = "lich chieu";

    private static final int UNICODE_REGEX_FLAGS =
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS;

    private static final String CODE_FENCE = "```";
    private static final String JSON_FENCE_LANG = "json";
    private static final Pattern DAYS_PATTERN = Pattern.compile(
        "(\\d{1,2})\\s*ngay",
        UNICODE_REGEX_FLAGS
    );
    private static final Pattern ROOM_DIM_PATTERN = Pattern.compile(
        "(\\d{1,2})\\s*(?:x|×|hang)\\s*(\\d{1,2})",
        UNICODE_REGEX_FLAGS
    );
    private static final Pattern DURATION_PATTERN = Pattern.compile(
        "(\\d{2,3})\\s*(?:phut|p\\b)",
        UNICODE_REGEX_FLAGS
    );
    private static final Pattern CLOCK_TIME_PATTERN = Pattern.compile(
        "(?<!\\d)([01]?\\d|2[0-3])[:h]([0-5]\\d)(?!\\d)",
        UNICODE_REGEX_FLAGS
    );
    private static final Pattern PRICE_PATTERN = Pattern.compile(
        "(\\d{2,3})\\s*k\\b|(\\d{5,6})\\s*(?:d|vnd)?",
        UNICODE_REGEX_FLAGS
    );

    private static final List<String> MOVIE_TITLE_STOP_MARKERS = List.of(
        ",", "\n", " đang chiếu", " dang chieu", " sắp chiếu", " sap chieu", " thể loại", " the loai"
    );
    private static final Set<String> ROOM_NAME_STOP_WORDS = Set.of(
        "co", "tai", "o", "rap", "hang", "cot", "ghe", "moi", "chieu"
    );

    private static final List<String> CINEMA_KEYWORDS =
        List.of("landmark", "galaxy", "nguyen du", "aeon", "tan phu");

    private static final String DEFAULT_HELP_MESSAGE = """
        Mình là **Trợ lý AI Quản trị CineX**. Bạn có thể yêu cầu mình:
        - **Tạo phòng chiếu:** *"Tạo phòng Screen 3 có 10 hàng 12 cột ở rạp Landmark 81"*
        - **Thêm phim mới:** *"Thêm phim Deadpool & Wolverine đang chiếu, 128 phút, 16+"*
        - **Tạo suất chiếu:** *"Thêm suất chiếu Dune: Part Two ngày mai lúc 18:00 và 21:00 ở Landmark 81 giá 95k"*
        - **Tự động phủ lịch 10 ngày:** *"Tự động cập nhật suất chiếu cho 10 ngày tới"*
        """.trim();

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
        String rawReply = queryModelSafely(systemPrompt, history, userMessage);

        List<JsonNode> actionNodes = rawReply.isBlank() ? List.of() : extractJsonActions(rawReply);
        String cleanReply = rawReply.isBlank() ? "" : stripJsonBlocks(rawReply).trim();

        List<AdminAiActionResult> executedActions = executeActionsOrFallback(actionNodes, userMessage);
        boolean dataChanged = executedActions.stream().anyMatch(AdminAiActionResult::success);
        String finalReply = finalizeReplyText(cleanReply, executedActions);

        return new AdminAiResponse(finalReply, nvidiaAiService.getModel(), executedActions, dataChanged);
    }

    private String queryModelSafely(String systemPrompt, List<AiChatMessage> history, String userMessage) {
        if (!nvidiaAiService.isConfigured()) {
            return "";
        }
        try {
            String reply = nvidiaAiService.chat(systemPrompt, history, userMessage);
            return reply == null ? "" : reply;
        } catch (Exception ex) {
            log.warn("Gọi NVIDIA AI trong Admin gặp lỗi, chuyển sang bộ phân tích lệnh trực tiếp: {}", ex.getMessage());
            return "";
        }
    }

    private List<AdminAiActionResult> executeActionsOrFallback(List<JsonNode> actionNodes, String userMessage) {
        if (actionNodes.isEmpty()) {
            return tryExecuteFallbackCommand(userMessage);
        }
        List<AdminAiActionResult> executed = new ArrayList<>();
        for (JsonNode node : actionNodes) {
            executed.addAll(executeActionNode(node));
        }
        return executed;
    }

    private String finalizeReplyText(String cleanReply, List<AdminAiActionResult> executedActions) {
        if (cleanReply.isBlank()) {
            return executedActions.isEmpty() ? DEFAULT_HELP_MESSAGE : buildSummaryFromActions(executedActions);
        }
        boolean allFailed = !executedActions.isEmpty()
            && executedActions.stream().noneMatch(AdminAiActionResult::success);
        return allFailed ? buildSummaryFromActions(executedActions) : cleanReply;
    }

    private String buildAdminContext() {
        LocalDateTime now = LocalDateTime.now(CINEMA_ZONE);
        LocalDate today = now.toLocalDate();
        StringBuilder sb = new StringBuilder(4096);

        sb.append("DỮ LIỆU HỆ THỐNG HIỆN TẠI:\n")
            .append("- Thời điểm hiện tại: ").append(now.format(NOW_FMT))
            .append(" (Hôm nay: ").append(today)
            .append(", Ngày mai: ").append(today.plusDays(1))
            .append(", Ngày kia: ").append(today.plusDays(2)).append(")\n");

        appendCinemasContext(sb);
        appendMoviesContext(sb);
        return sb.toString();
    }

    private void appendCinemasContext(StringBuilder sb) {
        sb.append("\nDANH SÁCH RẠP & PHÒNG CHIẾU:\n");
        for (Cinema cinema : cinemaRepository.findAll()) {
            sb.append("- [cinemaId=").append(cinema.getId()).append("] ")
                .append(cinema.getName())
                .append(cinema.isActive() ? " (Đang hoạt động)" : " (Bảo trì)")
                .append(" | Phòng chiếu: ")
                .append(formatAuditoriumList(cinema.getId()))
                .append('\n');
        }
    }

    private String formatAuditoriumList(Long cinemaId) {
        List<Auditorium> auds = auditoriumRepository.findByCinemaId(cinemaId);
        if (auds.isEmpty()) {
            return "chưa có phòng";
        }
        return auds.stream()
            .map(a -> String.format("[auditoriumId=%d] %s (%dx%d%s)",
                a.getId(), a.getName(), a.getTotalRows(), a.getColumnsPerRow(),
                a.isActive() ? "" : ", bảo trì"))
            .collect(Collectors.joining(", "));
    }

    private void appendMoviesContext(StringBuilder sb) {
        sb.append("\nDANH SÁCH PHIM:\n");
        for (Movie movie : movieRepository.findAll()) {
            int duration = movie.getDurationMinutes() == null ? 120 : movie.getDurationMinutes();
            sb.append("- [movieId=").append(movie.getId()).append("] ")
                .append(movie.getTitle())
                .append(" | ").append(duration).append(" phút")
                .append(" | ").append(movie.getStatus())
                .append(movie.isActive() ? "" : " (Đã ẩn)")
                .append('\n');
        }
    }

    private List<JsonNode> extractJsonActions(String text) {
        List<JsonNode> result = new ArrayList<>();
        for (FencedBlock block : findJsonFencedBlocks(text)) {
            parseActionsFromJsonString(block.json(), result);
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

    /** Khối ```json ... ``` trong câu trả lời của AI: vị trí [start, end) và nội dung JSON bên trong. */
    private record FencedBlock(int start, int end, String json) {
    }

    /**
     * Quét tuyến tính các khối code fence chứa JSON object (thay cho regex dễ bị backtracking).
     */
    private static List<FencedBlock> findJsonFencedBlocks(String text) {
        List<FencedBlock> blocks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return blocks;
        }
        int searchFrom = 0;
        while (searchFrom < text.length()) {
            int open = text.indexOf(CODE_FENCE, searchFrom);
            if (open < 0) {
                break;
            }
            int close = text.indexOf(CODE_FENCE, open + CODE_FENCE.length());
            if (close < 0) {
                break;
            }
            String content = text.substring(open + CODE_FENCE.length(), close).trim();
            if (content.regionMatches(true, 0, JSON_FENCE_LANG, 0, JSON_FENCE_LANG.length())) {
                content = content.substring(JSON_FENCE_LANG.length()).trim();
            }
            if (content.startsWith("{") && content.endsWith("}")) {
                blocks.add(new FencedBlock(open, close + CODE_FENCE.length(), content));
            }
            searchFrom = close + CODE_FENCE.length();
        }
        return blocks;
    }

    private void parseActionsFromJsonString(String jsonCandidate, List<JsonNode> result) {
        try {
            JsonNode root = objectMapper.readTree(jsonCandidate);
            JsonNode actions = root.path(FIELD_ACTIONS);
            if (actions.isArray()) {
                actions.forEach(result::add);
            } else if (root.has(FIELD_TYPE)) {
                result.add(root);
            }
        } catch (Exception ignored) {
            // Bỏ qua nếu đoạn JSON không hợp lệ
        }
    }

    private String stripJsonBlocks(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        int cursor = 0;
        for (FencedBlock block : findJsonFencedBlocks(text)) {
            sb.append(text, cursor, block.start());
            cursor = block.end();
        }
        sb.append(text.substring(cursor));
        return stripInlineActionsObjects(sb.toString()).trim();
    }

    /** Loại bỏ các object JSON dạng {"actions": [...]} nằm trần trong văn bản (đếm ngoặc tuyến tính). */
    private static String stripInlineActionsObjects(String text) {
        String current = text;
        int actionsIdx = current.indexOf("\"" + FIELD_ACTIONS + "\"");
        while (actionsIdx >= 0) {
            int open = current.lastIndexOf('{', actionsIdx);
            int close = open >= 0 ? findMatchingBrace(current, open) : -1;
            if (close < 0) {
                break;
            }
            current = current.substring(0, open) + current.substring(close + 1);
            actionsIdx = current.indexOf("\"" + FIELD_ACTIONS + "\"", open);
        }
        return current;
    }

    private static int findMatchingBrace(String text, int openIndex) {
        int depth = 0;
        boolean inString = false;
        for (int i = openIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
            } else if (!inString) {
                depth += braceDelta(c);
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int braceDelta(char c) {
        if (c == '{') {
            return 1;
        }
        return c == '}' ? -1 : 0;
    }

    private List<AdminAiActionResult> executeActionNode(JsonNode node) {
        String type = node.path(FIELD_TYPE).asText("").trim().toUpperCase(Locale.ROOT);
        try {
            return switch (type) {
                case ACTION_CREATE_AUDITORIUM -> List.of(handleCreateAuditorium(node));
                case ACTION_CREATE_CINEMA -> List.of(handleCreateCinema(node));
                case ACTION_CREATE_MOVIE -> List.of(handleCreateMovie(node));
                case ACTION_CREATE_SHOWTIME -> handleCreateShowtime(node);
                case ACTION_UPDATE_SHOWTIME -> List.of(handleUpdateShowtime(node));
                case ACTION_AUTO_GENERATE, "AUTO_EXTEND_SHOWTIMES" -> List.of(handleAutoGenerateShowtimes(node));
                default -> List.of();
            };
        } catch (Exception ex) {
            String reason = extractErrorMessage(ex);
            return List.of(new AdminAiActionResult(type, false, "Không thể thực hiện " + type, reason, null));
        }
    }

    private AdminAiActionResult handleCreateCinema(JsonNode node) {
        String name = node.path(FIELD_NAME).asText("").trim();
        if (name.isBlank()) {
            return new AdminAiActionResult(ACTION_CREATE_CINEMA, false, "Thiếu tên rạp", "Vui lòng cung cấp tên rạp cần tạo", null);
        }
        CinemaRequest req = new CinemaRequest();
        req.setName(name);
        req.setAddress(node.path("address").asText("TP. Hồ Chí Minh"));
        req.setPhone(node.path("phone").asText("1900 6017"));
        CinemaResponse created = cinemaService.create(req);
        return new AdminAiActionResult(
            ACTION_CREATE_CINEMA,
            true,
            "Đã tạo rạp: " + created.getName(),
            "Mã rạp #" + created.getId() + " · Địa chỉ: " + created.getAddress(),
            created.getId()
        );
    }

    private AdminAiActionResult handleCreateAuditorium(JsonNode node) {
        Cinema cinema = resolveCinema(
            node.path(FIELD_CINEMA_ID).isNumber() ? node.path(FIELD_CINEMA_ID).asLong() : null,
            node.path(FIELD_CINEMA_NAME).asText("")
        );
        if (cinema == null) {
            return new AdminAiActionResult(
                ACTION_CREATE_AUDITORIUM,
                false,
                "Không tìm thấy rạp chiếu",
                "Vui lòng nêu rõ tên rạp (ví dụ: Landmark 81, Galaxy Nguyễn Du, Aeon Tân Phú)",
                null
            );
        }

        String roomName = node.path(FIELD_NAME).asText("").trim();
        if (roomName.isBlank()) {
            roomName = defaultRoomName(cinema.getId());
        }
        int rows = Math.max(1, Math.min(30, node.path("totalRows").asInt(8)));
        int cols = Math.max(1, Math.min(30, node.path("columnsPerRow").asInt(12)));

        AuditoriumRequest req = new AuditoriumRequest();
        req.setName(roomName);
        req.setTotalRows(rows);
        req.setColumnsPerRow(cols);

        AuditoriumResponse created = auditoriumService.create(cinema.getId(), req);
        return new AdminAiActionResult(
            ACTION_CREATE_AUDITORIUM,
            true,
            "Đã tạo phòng chiếu: " + created.getName(),
            String.format("Rạp %s · %d hàng × %d cột (%d ghế tự động phân hạng)",
                created.getCinemaName(), created.getTotalRows(), created.getColumnsPerRow(), created.getSeatCount()),
            created.getId()
        );
    }

    private AdminAiActionResult handleCreateMovie(JsonNode node) {
        String title = node.path(FIELD_TITLE).asText("").trim().replaceAll("\\s+", " ");
        if (title.isBlank()) {
            return new AdminAiActionResult(ACTION_CREATE_MOVIE, false, "Thiếu tên phim", "Vui lòng nhập tên phim cần thêm", null);
        }

        Movie existingMovie = findExistingActiveMovieByTitle(title);
        if (existingMovie != null) {
            return new AdminAiActionResult(
                ACTION_CREATE_MOVIE,
                false,
                "Phim đã tồn tại: " + existingMovie.getTitle(),
                "Phim #" + existingMovie.getId() + " đã có trong danh mục, hệ thống không thêm trùng.",
                existingMovie.getId()
            );
        }

        MovieRequest req = buildMovieRequestFromNode(node, title);
        MovieResponse created = movieService.create(req);
        return new AdminAiActionResult(
            ACTION_CREATE_MOVIE,
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

    private Movie findExistingActiveMovieByTitle(String title) {
        return movieRepository.findAll().stream()
            .filter(Movie::isActive)
            .filter(m -> m.getTitle() != null && m.getTitle().trim().replaceAll("\\s+", " ").equalsIgnoreCase(title))
            .findFirst()
            .orElse(null);
    }

    private MovieRequest buildMovieRequestFromNode(JsonNode node, String title) {
        TmdbDtos.TmdbMovie match = findFirstTmdbMatch(title);
        String posterUrl = resolvePosterUrl(node.path("posterUrl").asText("").trim(), match);
        String description = resolveDescription(node.path("description").asText("").trim(), title, match);
        int duration = resolveDuration(node.path(FIELD_DURATION_MINUTES).asInt(0), match);
        String trailerUrl = resolveTrailerUrl(node.path("trailerUrl").asText("").trim(), title);
        String genre = node.path("genre").asText("").trim();
        String statusStr = node.path(FIELD_STATUS).asText(STATUS_NOW_SHOWING).trim().toUpperCase(Locale.ROOT);

        MovieRequest req = new MovieRequest();
        req.setTitle(title);
        req.setDescription(description);
        req.setPosterUrl(posterUrl);
        req.setTrailerUrl(trailerUrl);
        req.setDurationMinutes(duration);
        req.setAgeRating(node.path("ageRating").asInt(13));
        req.setGenre(genre.isBlank() ? "Hành động, Giải trí" : genre);
        req.setReleaseDate(LocalDate.now(CINEMA_ZONE));
        req.setStatus("COMING_SOON".equals(statusStr) ? MovieStatus.COMING_SOON : MovieStatus.NOW_SHOWING);
        return req;
    }

    private String resolvePosterUrl(String rawPoster, TmdbDtos.TmdbMovie match) {
        if (!rawPoster.isBlank()) {
            return rawPoster;
        }
        if (match != null && match.posterPath() != null) {
            return tmdbClient.posterUrl(match.posterPath());
        }
        return null;
    }

    private static String resolveDescription(String rawDescription, String title, TmdbDtos.TmdbMovie match) {
        if (!rawDescription.isBlank()) {
            return rawDescription;
        }
        if (match != null && match.overview() != null && !match.overview().isBlank()) {
            return match.overview();
        }
        return "Phim điện ảnh " + title + " trình chiếu tại hệ thống rạp CineX.";
    }

    private int resolveDuration(int rawDuration, TmdbDtos.TmdbMovie match) {
        if (rawDuration > 0) {
            return rawDuration;
        }
        if (match != null && match.id() != null) {
            int tmdbRuntime = fetchTmdbRuntime(match.id());
            if (tmdbRuntime > 0) {
                return tmdbRuntime;
            }
        }
        return 120;
    }

    private String resolveTrailerUrl(String rawTrailer, String title) {
        String candidate = rawTrailer.isBlank() ? lookupYoutubeEmbedUrl(title) : rawTrailer;
        return candidate.isBlank() ? null : candidate;
    }

    private TmdbDtos.TmdbMovie findFirstTmdbMatch(String title) {
        try {
            TmdbDtos.TmdbPage<TmdbDtos.TmdbMovie> searchPage = tmdbClient.search(title, 1);
            if (searchPage != null && searchPage.results() != null && !searchPage.results().isEmpty()) {
                return searchPage.results().get(0);
            }
        } catch (Exception ignored) {
            // Không chặn việc tạo phim nếu TMDB chưa cấu hình
        }
        return null;
    }

    private int fetchTmdbRuntime(long tmdbId) {
        try {
            TmdbDtos.TmdbMovie detail = tmdbClient.movieDetail(tmdbId);
            if (detail != null && detail.runtime() != null && detail.runtime() > 0) {
                return detail.runtime();
            }
        } catch (Exception ignored) {
            // Bỏ qua nếu không lấy được chi tiết runtime
        }
        return 0;
    }

    private String lookupYoutubeEmbedUrl(String title) {
        try {
            YoutubeDtos.YoutubeTrailer trailer = youtubeService.findFirstTrailer(title);
            if (trailer != null && trailer.embedUrl() != null) {
                return trailer.embedUrl();
            }
        } catch (Exception ignored) {
            // Không chặn việc tạo phim nếu YouTube chưa cấu hình
        }
        return "";
    }

    private List<AdminAiActionResult> handleCreateShowtime(JsonNode node) {
        Movie movie = resolveMovie(
            node.path(FIELD_MOVIE_ID).isNumber() ? node.path(FIELD_MOVIE_ID).asLong() : null,
            node.path("movieTitle").asText("")
        );
        if (movie == null) {
            return List.of(new AdminAiActionResult(
                ACTION_CREATE_SHOWTIME,
                false,
                "Không tìm thấy phim",
                "Vui lòng kiểm tra lại tên phim hoặc yêu cầu AI thêm phim trước khi xếp suất chiếu",
                null
            ));
        }

        Auditorium auditorium = resolveAuditorium(
            node.path(FIELD_AUDITORIUM_ID).isNumber() ? node.path(FIELD_AUDITORIUM_ID).asLong() : null,
            node.path(FIELD_AUDITORIUM_NAME).asText(""),
            node.path(FIELD_CINEMA_ID).isNumber() ? node.path(FIELD_CINEMA_ID).asLong() : null,
            node.path(FIELD_CINEMA_NAME).asText("")
        );
        if (auditorium == null) {
            return List.of(new AdminAiActionResult(
                ACTION_CREATE_SHOWTIME,
                false,
                "Không tìm thấy phòng chiếu phù hợp",
                "Vui lòng chỉ định rõ rạp hoặc phòng chiếu đang hoạt động",
                null
            ));
        }

        BigDecimal basePrice = normalizeBasePrice(node.path(FIELD_BASE_PRICE).asLong(90000));
        List<LocalDateTime> startTimes = extractStartTimes(node);
        if (startTimes.isEmpty()) {
            startTimes = List.of(LocalDate.now(CINEMA_ZONE).plusDays(1).atTime(19, 30));
        }
        return createShowtimesForSlots(movie, auditorium, startTimes, basePrice);
    }

    private List<AdminAiActionResult> createShowtimesForSlots(Movie movie, Auditorium auditorium,
                                                              List<LocalDateTime> startTimes, BigDecimal basePrice) {
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
                    ACTION_CREATE_SHOWTIME,
                    true,
                    "Đã tạo suất chiếu: " + created.getMovieTitle(),
                    String.format("%s · %s (%s ghế) · %s %s · %,dđ",
                        created.getCinemaName(),
                        created.getAuditoriumName(),
                        created.getTotalSeats(),
                        created.getStartTime().format(DATE_FMT),
                        created.getStartTime().format(TIME_FMT),
                        created.getBasePrice().longValue()),
                    created.getId()
                ));
            } catch (Exception ex) {
                results.add(new AdminAiActionResult(
                    ACTION_CREATE_SHOWTIME,
                    false,
                    "Không tạo được suất " + startTime.format(DATE_FMT) + " " + startTime.format(TIME_FMT),
                    extractErrorMessage(ex),
                    null
                ));
            }
        }
        return results;
    }

    private static BigDecimal normalizeBasePrice(long rawPrice) {
        BigDecimal price = BigDecimal.valueOf(rawPrice <= 0 ? 90000L : rawPrice);
        if (price.compareTo(BigDecimal.valueOf(1000)) < 0) {
            return price.multiply(BigDecimal.valueOf(1000));
        }
        return price;
    }

    private AdminAiActionResult handleUpdateShowtime(JsonNode node) {
        if (!node.path(FIELD_SHOWTIME_ID).isNumber()) {
            return new AdminAiActionResult(
                ACTION_UPDATE_SHOWTIME,
                false,
                "Thiếu mã suất chiếu (#ID)",
                "Vui lòng nêu mã suất chiếu cần cập nhật (ví dụ: suất chiếu #12)",
                null
            );
        }
        long showtimeId = node.path(FIELD_SHOWTIME_ID).asLong();
        Showtime existing = showtimeService.findById(showtimeId);

        ShowtimeRequest req = new ShowtimeRequest();
        req.setMovieId(node.path(FIELD_MOVIE_ID).isNumber() ? node.path(FIELD_MOVIE_ID).asLong() : existing.getMovie().getId());
        req.setAuditoriumId(node.path(FIELD_AUDITORIUM_ID).isNumber()
            ? node.path(FIELD_AUDITORIUM_ID).asLong()
            : existing.getAuditorium().getId());

        List<LocalDateTime> parsedTimes = extractStartTimes(node);
        req.setStartTime(parsedTimes.isEmpty() ? existing.getStartTime() : parsedTimes.get(0));
        req.setBasePrice(node.path(FIELD_BASE_PRICE).isNumber()
            ? normalizeBasePrice(node.path(FIELD_BASE_PRICE).asLong())
            : existing.getBasePrice());

        ShowtimeResponse updated = showtimeService.update(showtimeId, req);
        return new AdminAiActionResult(
            ACTION_UPDATE_SHOWTIME,
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
            ACTION_AUTO_GENERATE,
            true,
            String.format("Đã tự động xếp lịch chiếu %d ngày (%s – %s)",
                res.daysAhead(), res.fromDate().format(DATE_FMT), res.toDate().format(DATE_FMT)),
            String.format("Tạo mới %d suất chiếu · Chuyển %d suất đã qua sang Hoàn thành (COMPLETED)",
                res.createdCount(), res.completedCount()),
            null
        );
    }

    /**
     * Bộ phân tích dự phòng bằng biểu thức chính quy khi NVIDIA API không trả về JSON.
     */
    private List<AdminAiActionResult> tryExecuteFallbackCommand(String message) {
        if (message == null || message.isBlank()) {
            return List.of();
        }
        String norm = normalizeSearch(message);

        if (isAutoScheduleIntent(norm)) {
            return executeFallbackAutoSchedule(norm);
        }
        if (isCreateAuditoriumIntent(norm)) {
            return executeFallbackCreateAuditorium(message, norm);
        }
        if (isCreateMovieIntent(norm)) {
            return executeFallbackCreateMovie(message, norm);
        }
        if (norm.contains(TOKEN_SUAT_CHIEU) || norm.contains(TOKEN_LICH_CHIEU)) {
            return executeFallbackCreateShowtime(message, norm);
        }
        return List.of();
    }

    private static boolean isAutoScheduleIntent(String norm) {
        boolean hasVerb = norm.contains("tu dong") || norm.contains("cap nhat")
            || norm.contains("xep lich") || norm.contains("them lich");
        boolean hasRange = norm.contains("10 ngay") || norm.contains("5 ngay")
            || (norm.contains(TOKEN_SUAT_CHIEU) && norm.contains("ngay toi"));
        return hasVerb && hasRange;
    }

    private List<AdminAiActionResult> executeFallbackAutoSchedule(String norm) {
        int days = 10;
        Matcher m = DAYS_PATTERN.matcher(norm);
        if (m.find()) {
            days = Integer.parseInt(m.group(1));
        }
        ShowtimeService.AutoScheduleResult res = showtimeService.autoGenerateShowtimes(days);
        return List.of(new AdminAiActionResult(
            ACTION_AUTO_GENERATE,
            true,
            String.format("Đã tự động cập nhật suất chiếu cho %d ngày tới", res.daysAhead()),
            String.format("Từ %s đến %s · Thêm mới %d suất · Hoàn thành %d suất cũ",
                res.fromDate().format(DATE_FMT), res.toDate().format(DATE_FMT),
                res.createdCount(), res.completedCount()),
            null
        ));
    }

    private static boolean isCreateAuditoriumIntent(String norm) {
        return (norm.contains("tao phong") || norm.contains("them phong")) && !norm.contains(TOKEN_SUAT_CHIEU);
    }

    private List<AdminAiActionResult> executeFallbackCreateAuditorium(String message, String norm) {
        if (message == null || message.isBlank()) {
            return List.of();
        }
        Cinema cinema = resolveCinema(null, message);
        if (cinema == null) {
            return List.of();
        }
        int rows = 8;
        int cols = 12;
        Matcher dimMatcher = ROOM_DIM_PATTERN.matcher(norm);
        if (dimMatcher.find()) {
            rows = Integer.parseInt(dimMatcher.group(1));
            cols = Integer.parseInt(dimMatcher.group(2));
        }
        AuditoriumRequest req = new AuditoriumRequest();
        req.setName(extractRoomNameFromText(message, cinema.getId()));
        req.setTotalRows(Math.max(1, Math.min(30, rows)));
        req.setColumnsPerRow(Math.max(1, Math.min(30, cols)));
        AuditoriumResponse created = auditoriumService.create(cinema.getId(), req);
        return List.of(new AdminAiActionResult(
            ACTION_CREATE_AUDITORIUM,
            true,
            "Đã tạo phòng chiếu: " + created.getName(),
            String.format("Rạp %s · %d hàng × %d cột (%d ghế)",
                created.getCinemaName(), created.getTotalRows(), created.getColumnsPerRow(), created.getSeatCount()),
            created.getId()
        ));
    }

    private static boolean isCreateMovieIntent(String norm) {
        return (norm.contains("them phim") || norm.contains("tao phim"))
            && !norm.contains(TOKEN_SUAT_CHIEU) && !norm.contains(TOKEN_LICH_CHIEU);
    }

    private List<AdminAiActionResult> executeFallbackCreateMovie(String message, String norm) {
        String title = extractMovieTitleFromText(message);
        if (title.isBlank()) {
            return List.of();
        }
        int duration = 120;
        Matcher durMatcher = DURATION_PATTERN.matcher(norm);
        if (durMatcher.find()) {
            duration = Integer.parseInt(durMatcher.group(1));
        }
        MovieStatus status = norm.contains("sap chieu") ? MovieStatus.COMING_SOON : MovieStatus.NOW_SHOWING;
        JsonNode node = objectMapper.createObjectNode()
            .put(FIELD_TYPE, ACTION_CREATE_MOVIE)
            .put(FIELD_TITLE, title)
            .put(FIELD_DURATION_MINUTES, duration)
            .put(FIELD_STATUS, status.name());
        return List.of(handleCreateMovie(node));
    }

    private static String extractMovieTitleFromText(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        String quoted = extractFirstQuotedSegment(message);
        if (!quoted.isBlank()) {
            return quoted;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        int phimIdx = lower.indexOf("phim");
        if (phimIdx < 0) {
            return "";
        }
        String afterPhim = message.substring(phimIdx + "phim".length()).trim();
        String candidate = afterPhim;
        if (normalizeNonNull(afterPhim).startsWith("moi ")) {
            int spaceIdx = afterPhim.indexOf(' ');
            candidate = spaceIdx >= 0 ? afterPhim.substring(spaceIdx + 1).trim() : "";
        }
        return truncateAtStopMarker(candidate);
    }

    private static String extractFirstQuotedSegment(String text) {
        for (char quote : new char[]{'"', '\''}) {
            int first = text.indexOf(quote);
            int second = first >= 0 ? text.indexOf(quote, first + 1) : -1;
            if (second > first + 1) {
                return text.substring(first + 1, second).trim();
            }
        }
        return "";
    }

    private static String truncateAtStopMarker(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return "";
        }
        String lower = candidate.toLowerCase(Locale.ROOT);
        int cutIndex = candidate.length();
        for (String marker : MOVIE_TITLE_STOP_MARKERS) {
            int idx = lower.indexOf(marker);
            if (idx > 0 && idx < cutIndex) {
                cutIndex = idx;
            }
        }
        return stripSurroundingQuotes(candidate.substring(0, cutIndex).trim());
    }

    private static String stripSurroundingQuotes(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && isQuoteChar(value.charAt(start))) {
            start++;
        }
        while (end > start && isQuoteChar(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(start, end).trim();
    }

    private static boolean isQuoteChar(char c) {
        return c == '"' || c == '\'';
    }

    private List<AdminAiActionResult> executeFallbackCreateShowtime(String message, String norm) {
        Movie movie = resolveMovie(null, message);
        Auditorium auditorium = resolveAuditorium(null, message, null, message);
        if (movie == null || auditorium == null) {
            return List.of();
        }
        LocalDate targetDate = resolveFallbackTargetDate(norm);
        List<LocalDateTime> startTimes = extractFallbackClockTimes(norm).stream()
            .map(targetDate::atTime)
            .toList();
        BigDecimal price = parseFallbackPrice(norm);
        return createShowtimesForSlots(movie, auditorium, startTimes, price);
    }

    private static LocalDate resolveFallbackTargetDate(String norm) {
        LocalDate today = LocalDate.now(CINEMA_ZONE);
        if (norm.contains("ngay mai") || norm.contains("mai")) {
            return today.plusDays(1);
        }
        if (norm.contains("ngay kia") || norm.contains("mot")) {
            return today.plusDays(2);
        }
        return today;
    }

    private static List<LocalTime> extractFallbackClockTimes(String norm) {
        List<LocalTime> times = new ArrayList<>();
        Matcher timeMatcher = CLOCK_TIME_PATTERN.matcher(norm);
        while (timeMatcher.find()) {
            times.add(LocalTime.of(
                Integer.parseInt(timeMatcher.group(1)),
                Integer.parseInt(timeMatcher.group(2))
            ));
        }
        if (times.isEmpty()) {
            times.add(LocalTime.of(19, 30));
        }
        return times;
    }

    private static BigDecimal parseFallbackPrice(String norm) {
        Matcher priceMatcher = PRICE_PATTERN.matcher(norm);
        if (priceMatcher.find()) {
            if (priceMatcher.group(1) != null) {
                return BigDecimal.valueOf(Long.parseLong(priceMatcher.group(1)) * 1000L);
            }
            if (priceMatcher.group(2) != null) {
                return BigDecimal.valueOf(Long.parseLong(priceMatcher.group(2)));
            }
        }
        return new BigDecimal("90000");
    }

    private String extractRoomNameFromText(String text, Long cinemaId) {
        if (text == null || text.isBlank()) {
            return defaultRoomName(cinemaId);
        }
        String[] tokens = text.trim().split("\\s+");
        for (int i = 0; i < tokens.length - 1; i++) {
            String resolved = resolveRoomTokenPair(tokens, i);
            if (!resolved.isBlank()) {
                return resolved;
            }
        }
        return defaultRoomName(cinemaId);
    }

    private static String resolveRoomTokenPair(String[] tokens, int index) {
        if (tokens == null || index < 0 || index + 1 >= tokens.length) {
            return "";
        }
        String current = tokens[index];
        String next = tokens[index + 1];
        if (current == null || next == null) {
            return "";
        }
        String normCurrent = normalizeNonNull(current);
        String nextRaw = cleanRoomToken(next);
        if ("screen".equals(normCurrent)) {
            return nextRaw.isBlank() ? "" : SCREEN_PREFIX + nextRaw;
        }
        if (!"phong".equals(normCurrent)) {
            return "";
        }
        String normNext = normalizeNonNull(nextRaw);
        if ("screen".equals(normNext)) {
            String afterScreen = index + 2 < tokens.length ? cleanRoomToken(tokens[index + 2]) : "";
            return afterScreen.isBlank() ? "" : SCREEN_PREFIX + afterScreen;
        }
        if (normNext.isBlank() || ROOM_NAME_STOP_WORDS.contains(normNext) || normNext.matches("\\d+x\\d+")) {
            return "";
        }
        return nextRaw.matches("\\d+") ? SCREEN_PREFIX + nextRaw : nextRaw;
    }

    private static String cleanRoomToken(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("[,.\"']", "").trim();
    }

    private String defaultRoomName(Long cinemaId) {
        int next = auditoriumRepository.findByCinemaId(cinemaId).size() + 1;
        return SCREEN_PREFIX + next;
    }

    private List<LocalDateTime> extractStartTimes(JsonNode node) {
        List<LocalDateTime> times = new ArrayList<>();
        if (node.path(FIELD_START_TIMES).isArray()) {
            node.path(FIELD_START_TIMES).forEach(item -> addParsedTime(times, item.asText("")));
        }
        if (times.isEmpty()) {
            addParsedTime(times, node.path(FIELD_START_TIME).asText(""));
        }
        if (times.isEmpty() && node.path(FIELD_TIMES).isArray()) {
            String dateStr = node.path("date").asText("").trim();
            if (!dateStr.isBlank()) {
                node.path(FIELD_TIMES).forEach(t -> addParsedTime(times, dateStr + "T" + t.asText("").trim()));
            }
        }
        return times;
    }

    private void addParsedTime(List<LocalDateTime> times, String raw) {
        LocalDateTime dt = parseDateTimeFlexible(raw);
        if (dt != null) {
            times.add(dt);
        }
    }

    private LocalDateTime parseDateTimeFlexible(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
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
            if (byId != null) {
                return byId;
            }
        }
        String normQuery = normalizeSearch(cinemaQuery);
        List<Cinema> cinemas = cinemaRepository.findAll();
        if (!normQuery.isBlank()) {
            Cinema matched = cinemas.stream()
                .filter(c -> matchesCinemaQuery(normalizeSearch(c.getName()), normQuery))
                .findFirst()
                .orElse(null);
            if (matched != null) {
                return matched;
            }
        }
        return cinemas.stream().filter(Cinema::isActive).findFirst().orElse(null);
    }

    private static boolean matchesCinemaQuery(String normName, String normQuery) {
        if (normName.contains(normQuery) || normQuery.contains(normName)) {
            return true;
        }
        return CINEMA_KEYWORDS.stream()
            .anyMatch(token -> normQuery.contains(token) && normName.contains(token));
    }

    private Auditorium resolveAuditorium(Long auditoriumId, String auditoriumQuery,
                                         Long cinemaId, String cinemaQuery) {
        if (auditoriumId != null) {
            Auditorium byId = auditoriumRepository.findById(auditoriumId).orElse(null);
            if (byId != null && byId.isActive()) {
                return byId;
            }
        }
        String effectiveCinemaQuery = (cinemaQuery != null && !cinemaQuery.isBlank()) ? cinemaQuery : auditoriumQuery;
        Cinema cinema = resolveCinema(cinemaId, effectiveCinemaQuery);
        List<Auditorium> candidates = cinema != null
            ? auditoriumRepository.findByCinemaIdAndActiveTrue(cinema.getId())
            : auditoriumRepository.findAll().stream().filter(Auditorium::isActive).toList();

        String normAud = normalizeSearch(auditoriumQuery);
        if (!normAud.isBlank()) {
            Auditorium matched = candidates.stream()
                .filter(a -> matchesName(normalizeSearch(a.getName()), normAud))
                .findFirst()
                .orElse(null);
            if (matched != null) {
                return matched;
            }
        }
        return candidates.stream()
            .min(Comparator.comparing(Auditorium::getId))
            .orElse(null);
    }

    private static boolean matchesName(String normName, String normQuery) {
        return !normName.isBlank()
            && (normName.equals(normQuery) || normQuery.contains(normName) || normName.contains(normQuery));
    }

    private Movie resolveMovie(Long movieId, String movieQuery) {
        if (movieId != null) {
            Movie byId = movieRepository.findById(movieId).orElse(null);
            if (byId != null) {
                return byId;
            }
        }
        String normQuery = normalizeSearch(movieQuery);
        if (normQuery.isBlank()) {
            return null;
        }
        List<Movie> movies = movieRepository.findAll();
        return movies.stream()
            .filter(m -> matchesName(normalizeSearch(m.getTitle()), normQuery))
            .findFirst()
            .orElseGet(() -> findMovieByKeywordToken(movies, normQuery));
    }

    private static Movie findMovieByKeywordToken(List<Movie> movies, String normQuery) {
        for (Movie m : movies) {
            for (String word : normalizeSearch(m.getTitle()).split("\\s+")) {
                if (word.length() >= 4 && normQuery.contains(word)) {
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
        if (input == null) {
            return "";
        }
        return normalizeNonNull(input);
    }

    /**
     * Bỏ dấu tiếng Việt + lowercase cho chuỗi chắc chắn khác null.
     * Tách riêng để bộ phân tích tĩnh không suy luận ngược rằng đối số có thể null.
     */
    private static String normalizeNonNull(String input) {
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
