package com.cinex.cinema.admin;

import com.cinex.cinema.ai.NvidiaAiService;
import com.cinex.cinema.cinema.Auditorium;
import com.cinex.cinema.cinema.AuditoriumRepository;
import com.cinex.cinema.cinema.AuditoriumRequest;
import com.cinex.cinema.cinema.AuditoriumResponse;
import com.cinex.cinema.cinema.AuditoriumService;
import com.cinex.cinema.cinema.Cinema;
import com.cinex.cinema.cinema.CinemaRepository;
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
import com.cinex.cinema.trailer.YoutubeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử luồng chat của {@link AdminAiService}: thực thi action JSON từ AI
 * và bộ phân tích lệnh dự phòng khi AI không khả dụng.
 */
class AdminAiServiceFlowTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private NvidiaAiService nvidiaAiService;
    private CinemaRepository cinemaRepository;
    private CinemaService cinemaService;
    private AuditoriumRepository auditoriumRepository;
    private AuditoriumService auditoriumService;
    private MovieRepository movieRepository;
    private MovieService movieService;
    private ShowtimeService showtimeService;
    private AdminAiService adminAiService;

    private Cinema cinema;
    private Auditorium auditorium;
    private Movie movie;

    @BeforeEach
    void setUp() {
        nvidiaAiService = mock(NvidiaAiService.class);
        cinemaRepository = mock(CinemaRepository.class);
        cinemaService = mock(CinemaService.class);
        auditoriumRepository = mock(AuditoriumRepository.class);
        auditoriumService = mock(AuditoriumService.class);
        movieRepository = mock(MovieRepository.class);
        movieService = mock(MovieService.class);
        showtimeService = mock(ShowtimeService.class);
        TmdbClient tmdbClient = mock(TmdbClient.class);
        YoutubeService youtubeService = mock(YoutubeService.class);

        cinema = Cinema.builder().name("Cinex Landmark 81").active(true).build();
        cinema.setId(1L);
        auditorium = Auditorium.builder().name("Screen 1").cinema(cinema)
            .totalRows(8).columnsPerRow(12).active(true).build();
        auditorium.setId(10L);
        movie = Movie.builder().title("Dune").durationMinutes(150)
            .status(MovieStatus.NOW_SHOWING).active(true).build();
        movie.setId(100L);

        when(cinemaRepository.findAll()).thenReturn(List.of(cinema));
        when(cinemaRepository.findById(1L)).thenReturn(Optional.of(cinema));
        when(auditoriumRepository.findByCinemaId(1L)).thenReturn(List.of(auditorium));
        when(auditoriumRepository.findByCinemaIdAndActiveTrue(1L)).thenReturn(List.of(auditorium));
        when(auditoriumRepository.findById(10L)).thenReturn(Optional.of(auditorium));
        when(movieRepository.findAll()).thenReturn(List.of(movie));
        when(movieRepository.findById(100L)).thenReturn(Optional.of(movie));
        when(nvidiaAiService.getModel()).thenReturn("test-model");

        adminAiService = new AdminAiService(
            nvidiaAiService, cinemaRepository, cinemaService,
            auditoriumRepository, auditoriumService,
            movieRepository, movieService, showtimeService,
            tmdbClient, youtubeService, new ObjectMapper()
        );
    }

    private static ShowtimeResponse showtimeResponse(long id, LocalDateTime start) {
        return ShowtimeResponse.builder()
            .id(id).movieTitle("Dune").cinemaName("Cinex Landmark 81").auditoriumName("Screen 1")
            .startTime(start).basePrice(new BigDecimal("95000")).totalSeats(96)
            .build();
    }

    private static ShowtimeService.AutoScheduleResult autoResult(int days) {
        LocalDate today = LocalDate.now(ZONE);
        return new ShowtimeService.AutoScheduleResult(12, 2, today, today.plusDays(days - 1L), days);
    }

    // ======================== Bộ phân tích dự phòng ========================

    @Test
    void blankMessageWithoutAiReturnsHelpMessage() {
        AdminAiResponse response = adminAiService.chat("   ", List.of());

        assertThat(response.actions()).isEmpty();
        assertThat(response.dataChanged()).isFalse();
        assertThat(response.reply()).contains("Trợ lý AI Quản trị CineX");
    }

    @Test
    void fallbackAutoScheduleParsesNumberOfDays() {
        when(showtimeService.autoGenerateShowtimes(5)).thenReturn(autoResult(5));

        AdminAiResponse response = adminAiService.chat("Tự động cập nhật suất chiếu cho 5 ngày tới", List.of());

        verify(showtimeService).autoGenerateShowtimes(5);
        assertThat(response.dataChanged()).isTrue();
        assertThat(response.actions()).singleElement()
            .satisfies(a -> assertThat(a.title()).contains("5 ngày tới"));
        assertThat(response.reply()).contains("Mình đã xử lý yêu cầu");
    }

    @Test
    void fallbackCreateAuditoriumParsesNameAndDimensions() {
        when(auditoriumService.create(eq(1L), any())).thenAnswer(inv -> {
            AuditoriumRequest req = inv.getArgument(1);
            return AuditoriumResponse.builder().id(7L).name(req.getName()).cinemaName("Cinex Landmark 81")
                .totalRows(req.getTotalRows()).columnsPerRow(req.getColumnsPerRow())
                .seatCount(req.getTotalRows() * req.getColumnsPerRow()).build();
        });

        AdminAiResponse response = adminAiService.chat("Tạo phòng Screen 7 có 9x14 ở rạp Landmark 81", List.of());

        ArgumentCaptor<AuditoriumRequest> captor = ArgumentCaptor.forClass(AuditoriumRequest.class);
        verify(auditoriumService).create(eq(1L), captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Screen 7");
        assertThat(captor.getValue().getTotalRows()).isEqualTo(9);
        assertThat(captor.getValue().getColumnsPerRow()).isEqualTo(14);
        assertThat(response.actions().get(0).title()).contains("Screen 7");
    }

    @Test
    void fallbackCreateAuditoriumUsesCustomOrDefaultName() {
        when(auditoriumService.create(eq(1L), any())).thenAnswer(inv -> {
            AuditoriumRequest req = inv.getArgument(1);
            return AuditoriumResponse.builder().id(8L).name(req.getName()).cinemaName("Cinex Landmark 81")
                .totalRows(req.getTotalRows()).columnsPerRow(req.getColumnsPerRow()).seatCount(96).build();
        });

        adminAiService.chat("Thêm phòng VIP1 tại rạp Landmark", List.of());
        adminAiService.chat("Tạo phòng mới ở Landmark", List.of());

        ArgumentCaptor<AuditoriumRequest> captor = ArgumentCaptor.forClass(AuditoriumRequest.class);
        verify(auditoriumService, org.mockito.Mockito.times(2)).create(eq(1L), captor.capture());
        assertThat(captor.getAllValues().get(0).getName()).isEqualTo("VIP1");
        // Không có tên phòng → đặt theo số thứ tự kế tiếp
        assertThat(captor.getAllValues().get(1).getName()).isEqualTo("Screen 2");
        assertThat(captor.getAllValues().get(1).getTotalRows()).isEqualTo(8);
    }

    @Test
    void fallbackCreateMovieParsesTitleDurationAndStatus() {
        when(movieService.create(any())).thenAnswer(inv -> {
            MovieRequest req = inv.getArgument(0);
            return MovieResponse.builder().id(55L).title(req.getTitle())
                .durationMinutes(req.getDurationMinutes()).genre(req.getGenre()).status(req.getStatus()).build();
        });

        AdminAiResponse response = adminAiService.chat("Thêm phim mới Oppenheimer sắp chiếu 180 phút", List.of());

        ArgumentCaptor<MovieRequest> captor = ArgumentCaptor.forClass(MovieRequest.class);
        verify(movieService).create(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Oppenheimer");
        assertThat(captor.getValue().getDurationMinutes()).isEqualTo(180);
        assertThat(captor.getValue().getStatus()).isEqualTo(MovieStatus.COMING_SOON);
        assertThat(captor.getValue().getDescription()).contains("Oppenheimer");
        assertThat(response.actions().get(0).detail()).contains("Sắp chiếu");
    }

    @Test
    void fallbackCreateMovieRejectsDuplicateTitle() {
        AdminAiResponse response = adminAiService.chat("Thêm phim \"dune\"", List.of());

        verify(movieService, never()).create(any());
        assertThat(response.dataChanged()).isFalse();
        assertThat(response.actions().get(0).title()).contains("Phim đã tồn tại");
    }

    @Test
    void fallbackCreateShowtimeParsesTimesPriceAndReportsFailures() {
        LocalDate tomorrow = LocalDate.now(ZONE).plusDays(1);
        when(showtimeService.create(any())).thenAnswer(inv -> {
            ShowtimeRequest req = inv.getArgument(0);
            if (req.getStartTime().getHour() == 21) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Phòng đã có suất chiếu");
            }
            return showtimeResponse(500L, req.getStartTime());
        });

        AdminAiResponse response = adminAiService.chat(
            "Thêm suất chiếu Dune ngày mai lúc 18:00 và 21h30 ở Landmark 81 giá 95k", List.of());

        ArgumentCaptor<ShowtimeRequest> captor = ArgumentCaptor.forClass(ShowtimeRequest.class);
        verify(showtimeService, org.mockito.Mockito.times(2)).create(captor.capture());
        assertThat(captor.getAllValues().get(0).getStartTime()).isEqualTo(tomorrow.atTime(18, 0));
        assertThat(captor.getAllValues().get(1).getStartTime()).isEqualTo(tomorrow.atTime(21, 30));
        assertThat(captor.getAllValues().get(0).getBasePrice()).isEqualByComparingTo("95000");
        assertThat(response.actions()).hasSize(2);
        assertThat(response.actions().get(0).success()).isTrue();
        assertThat(response.actions().get(1).success()).isFalse();
        assertThat(response.actions().get(1).detail()).isEqualTo("Phòng đã có suất chiếu");
    }

    @Test
    void aiErrorFallsBackToDirectCommandParser() {
        when(nvidiaAiService.isConfigured()).thenReturn(true);
        when(nvidiaAiService.chat(any(), any(), any())).thenThrow(new IllegalStateException("timeout"));
        when(showtimeService.autoGenerateShowtimes(10)).thenReturn(autoResult(10));

        AdminAiResponse response = adminAiService.chat("Tự động xếp lịch 10 ngày", List.of());

        verify(showtimeService).autoGenerateShowtimes(10);
        assertThat(response.dataChanged()).isTrue();
    }

    // ======================== Thực thi action JSON ========================

    @Test
    void executesCinemaUpdateShowtimeAndAutoExtendActionsFromFencedJson() {
        Showtime existing = Showtime.builder().movie(movie).auditorium(auditorium)
            .startTime(LocalDateTime.now(ZONE).plusDays(2)).basePrice(new BigDecimal("90000")).build();
        existing.setId(12L);
        when(showtimeService.findById(12L)).thenReturn(existing);
        when(showtimeService.update(eq(12L), any())).thenAnswer(inv -> {
            ShowtimeRequest req = inv.getArgument(1);
            ShowtimeResponse resp = showtimeResponse(12L, req.getStartTime());
            resp.setBasePrice(req.getBasePrice());
            return resp;
        });
        when(cinemaService.create(any())).thenReturn(
            CinemaResponse.builder().id(4L).name("Cinex Thủ Đức").address("TP. Hồ Chí Minh").build());
        when(showtimeService.autoGenerateShowtimes(anyInt())).thenReturn(autoResult(7));

        when(nvidiaAiService.isConfigured()).thenReturn(true);
        when(nvidiaAiService.chat(any(), any(), any())).thenReturn("""
            Đã xử lý xong các yêu cầu.
            ```json
            {"actions": [
              {"type": "CREATE_CINEMA", "name": "Cinex Thủ Đức"},
              {"type": "CREATE_CINEMA", "name": ""},
              {"type": "UPDATE_SHOWTIME", "showtimeId": 12, "startTimes": ["2030-01-05 20:15"], "basePrice": 120},
              {"type": "UPDATE_SHOWTIME"},
              {"type": "AUTO_EXTEND_SHOWTIMES", "days": 7},
              {"type": "UNKNOWN_ACTION"}
            ]}
            ```
            """);

        AdminAiResponse response = adminAiService.chat("Cập nhật dữ liệu", List.of());

        assertThat(response.reply()).isEqualTo("Đã xử lý xong các yêu cầu.");
        assertThat(response.actions()).hasSize(5);
        assertThat(response.actions().get(0).success()).isTrue();
        assertThat(response.actions().get(1).title()).isEqualTo("Thiếu tên rạp");
        assertThat(response.actions().get(2).success()).isTrue();
        assertThat(response.actions().get(2).detail()).contains("05/01/2030").contains("20:15").contains("120");
        assertThat(response.actions().get(3).success()).isFalse();
        assertThat(response.actions().get(4).title()).contains("7 ngày");

        ArgumentCaptor<ShowtimeRequest> captor = ArgumentCaptor.forClass(ShowtimeRequest.class);
        verify(showtimeService).update(eq(12L), captor.capture());
        assertThat(captor.getValue().getMovieId()).isEqualTo(100L);
        assertThat(captor.getValue().getAuditoriumId()).isEqualTo(10L);
        assertThat(captor.getValue().getBasePrice()).isEqualByComparingTo("120000");
    }

    @Test
    void executesCreateShowtimeFromInlineJsonAndSummarizesWhenAllFail() {
        when(showtimeService.create(any())).thenThrow(new IllegalArgumentException("Khung giờ không hợp lệ"));

        when(nvidiaAiService.isConfigured()).thenReturn(true);
        when(nvidiaAiService.chat(any(), any(), any())).thenReturn(
            "Mình sẽ tạo suất chiếu {\"actions\": [{\"type\": \"CREATE_SHOWTIME\", \"movieTitle\": \"Dune\","
                + " \"cinemaName\": \"Landmark\", \"date\": \"2030-02-01\", \"times\": [\"18:00\", \"bad\"]},"
                + " {\"type\": \"CREATE_SHOWTIME\", \"movieTitle\": \"Phim Không Có\"}]} nhé");

        AdminAiResponse response = adminAiService.chat("Tạo suất chiếu", List.of());

        verify(showtimeService).create(any());
        assertThat(response.actions()).hasSize(2);
        assertThat(response.actions()).allMatch(a -> !a.success());
        assertThat(response.actions().get(0).detail()).isEqualTo("Khung giờ không hợp lệ");
        assertThat(response.actions().get(1).title()).isEqualTo("Không tìm thấy phim");
        // Tất cả action đều thất bại → trả về bản tóm tắt thay vì lời AI
        assertThat(response.reply()).contains("Mình đã xử lý yêu cầu").doesNotContain("actions");
        assertThat(response.dataChanged()).isFalse();
    }

    @Test
    void createAuditoriumActionReportsMissingCinema() {
        when(cinemaRepository.findAll()).thenReturn(List.of());
        when(cinemaRepository.findById(anyLong())).thenReturn(Optional.empty());
        when(nvidiaAiService.isConfigured()).thenReturn(true);
        when(nvidiaAiService.chat(any(), any(), any())).thenReturn(
            "```json\n{\"type\": \"CREATE_AUDITORIUM\", \"cinemaId\": 99, \"name\": \"Screen 9\"}\n```");

        AdminAiResponse response = adminAiService.chat("Tạo phòng", List.of());

        verify(auditoriumService, never()).create(anyLong(), any());
        assertThat(response.actions()).singleElement()
            .satisfies(a -> assertThat(a.title()).isEqualTo("Không tìm thấy rạp chiếu"));
    }
}
