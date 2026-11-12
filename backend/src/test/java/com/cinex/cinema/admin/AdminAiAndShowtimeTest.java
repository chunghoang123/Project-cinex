package com.cinex.cinema.admin;

import com.cinex.cinema.ai.NvidiaAiService;
import com.cinex.cinema.booking.BookingRepository;
import com.cinex.cinema.cinema.Auditorium;
import com.cinex.cinema.cinema.AuditoriumRepository;
import com.cinex.cinema.cinema.AuditoriumResponse;
import com.cinex.cinema.cinema.AuditoriumService;
import com.cinex.cinema.cinema.Cinema;
import com.cinex.cinema.cinema.CinemaRepository;
import com.cinex.cinema.cinema.CinemaService;
import com.cinex.cinema.cinema.Seat;
import com.cinex.cinema.cinema.SeatRepository;
import com.cinex.cinema.movie.Movie;
import com.cinex.cinema.movie.MovieRepository;
import com.cinex.cinema.movie.MovieRequest;
import com.cinex.cinema.movie.MovieResponse;
import com.cinex.cinema.movie.MovieService;
import com.cinex.cinema.movie.MovieStatus;
import com.cinex.cinema.trailer.YoutubeEmbedUrlNormalizer;
import com.cinex.cinema.showtime.Showtime;
import com.cinex.cinema.showtime.ShowtimeRepository;
import com.cinex.cinema.showtime.ShowtimeRequest;
import com.cinex.cinema.showtime.ShowtimeSeatRepository;
import com.cinex.cinema.showtime.ShowtimeService;
import com.cinex.cinema.showtime.ShowtimeStatus;
import com.cinex.cinema.tmdb.TmdbClient;
import com.cinex.cinema.trailer.YoutubeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminAiAndShowtimeTest {

    @Test
    void autoGenerateShowtimesCoversTenDaysAndCompletesPastShowtimes() {
        ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
        MovieRepository movieRepository = mock(MovieRepository.class);
        AuditoriumRepository auditoriumRepository = mock(AuditoriumRepository.class);
        SeatRepository seatRepository = mock(SeatRepository.class);
        ShowtimeSeatRepository showtimeSeatRepository = mock(ShowtimeSeatRepository.class);
        BookingRepository bookingRepository = mock(BookingRepository.class);

        ShowtimeService showtimeService = new ShowtimeService(
            showtimeRepository, movieRepository, auditoriumRepository,
            seatRepository, showtimeSeatRepository, bookingRepository
        );

        Cinema cinema = Cinema.builder().name("Cinex Landmark 81").active(true).build();
        cinema.setId(1L);
        Auditorium auditorium = Auditorium.builder().name("Screen 1").cinema(cinema).active(true).build();
        auditorium.setId(10L);

        Movie movie = Movie.builder()
            .title("Dune: Part Two")
            .durationMinutes(120)
            .status(MovieStatus.NOW_SHOWING)
            .active(true)
            .build();
        movie.setId(100L);

        Showtime pastShowtime = Showtime.builder()
            .movie(movie)
            .auditorium(auditorium)
            .startTime(LocalDateTime.now().minusHours(5))
            .endTime(LocalDateTime.now().minusHours(3))
            .basePrice(new BigDecimal("90000"))
            .status(ShowtimeStatus.ACTIVE)
            .active(true)
            .build();

        when(showtimeRepository.findByStatusAndEndTimeBefore(eq(ShowtimeStatus.ACTIVE), any()))
            .thenReturn(List.of(pastShowtime));
        when(movieRepository.findByStatusAndActiveTrue(MovieStatus.NOW_SHOWING))
            .thenReturn(List.of(movie));
        when(auditoriumRepository.findAll())
            .thenReturn(List.of(auditorium));
        when(seatRepository.findByAuditoriumIdAndActiveTrue(10L))
            .thenReturn(List.of(Seat.builder().seatCode("A1").seatType("STANDARD").rowNumber(1).columnNumber(1).active(true).build()));
        when(showtimeRepository.findByAuditoriumIdAndActiveTrue(10L))
            .thenReturn(new ArrayList<>());
        when(showtimeRepository.save(any(Showtime.class)))
            .thenAnswer(inv -> {
                Showtime s = inv.getArgument(0);
                s.setId(999L);
                return s;
            });

        ShowtimeService.AutoScheduleResult result = showtimeService.autoGenerateShowtimes(10);

        assertThat(result.daysAhead()).isEqualTo(10);
        assertThat(result.completedCount()).isEqualTo(1);
        assertThat(pastShowtime.getStatus()).isEqualTo(ShowtimeStatus.COMPLETED);
        assertThat(result.createdCount()).isGreaterThanOrEqualTo(27);
        assertThat(result.toDate()).isEqualTo(LocalDate.now().plusDays(9));
    }

    @Test
    void createShowtimeRejectsOverlappingSlotInSameAuditorium() {
        ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
        MovieRepository movieRepository = mock(MovieRepository.class);
        AuditoriumRepository auditoriumRepository = mock(AuditoriumRepository.class);
        SeatRepository seatRepository = mock(SeatRepository.class);
        ShowtimeSeatRepository showtimeSeatRepository = mock(ShowtimeSeatRepository.class);
        BookingRepository bookingRepository = mock(BookingRepository.class);

        ShowtimeService showtimeService = new ShowtimeService(
            showtimeRepository, movieRepository, auditoriumRepository,
            seatRepository, showtimeSeatRepository, bookingRepository
        );

        Cinema cinema = Cinema.builder().name("Cinex Landmark 81").active(true).build();
        Auditorium auditorium = Auditorium.builder().name("Screen 1").cinema(cinema).active(true).build();
        auditorium.setId(10L);
        Movie movie = Movie.builder().title("Interstellar").durationMinutes(150).status(MovieStatus.NOW_SHOWING).active(true).build();
        movie.setId(1L);

        LocalDateTime existingStart = LocalDate.now().plusDays(1).atTime(18, 0);
        Showtime existing = Showtime.builder()
            .movie(movie)
            .auditorium(auditorium)
            .startTime(existingStart)
            .endTime(existingStart.plusMinutes(150))
            .status(ShowtimeStatus.ACTIVE)
            .active(true)
            .build();
        existing.setId(50L);

        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(auditoriumRepository.findById(10L)).thenReturn(Optional.of(auditorium));
        when(showtimeRepository.findByAuditoriumIdAndActiveTrue(10L)).thenReturn(List.of(existing));

        ShowtimeRequest req = new ShowtimeRequest();
        req.setMovieId(1L);
        req.setAuditoriumId(10L);
        req.setStartTime(LocalDate.now().plusDays(1).atTime(19, 30));
        req.setBasePrice(new BigDecimal("90000"));

        assertThatThrownBy(() -> showtimeService.create(req))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("đã có suất chiếu");
    }

    @Test
    void adminAiServiceExecutesJsonActionsForAuditoriumAndMovie() {
        NvidiaAiService nvidiaAiService = mock(NvidiaAiService.class);
        CinemaRepository cinemaRepository = mock(CinemaRepository.class);
        CinemaService cinemaService = mock(CinemaService.class);
        AuditoriumRepository auditoriumRepository = mock(AuditoriumRepository.class);
        AuditoriumService auditoriumService = mock(AuditoriumService.class);
        MovieRepository movieRepository = mock(MovieRepository.class);
        MovieService movieService = mock(MovieService.class);
        ShowtimeService showtimeService = mock(ShowtimeService.class);
        TmdbClient tmdbClient = mock(TmdbClient.class);
        YoutubeService youtubeService = mock(YoutubeService.class);

        Cinema cinema = Cinema.builder().name("Cinex Landmark 81").active(true).build();
        cinema.setId(1L);
        when(cinemaRepository.findAll()).thenReturn(List.of(cinema));
        when(cinemaRepository.findById(1L)).thenReturn(Optional.of(cinema));
        when(auditoriumRepository.findByCinemaId(1L)).thenReturn(List.of());
        when(movieRepository.findAll()).thenReturn(List.of());

        when(nvidiaAiService.isConfigured()).thenReturn(true);
        when(nvidiaAiService.getModel()).thenReturn("nvidia/nemotron");
        when(nvidiaAiService.chat(any(), any(), any())).thenReturn("""
            Mình đã tạo phòng Screen 3 và thêm phim mới cho bạn.
            ```json
            {
              "actions": [
                {
                  "type": "CREATE_AUDITORIUM",
                  "cinemaId": 1,
                  "name": "Screen 3",
                  "totalRows": 10,
                  "columnsPerRow": 12
                },
                {
                  "type": "CREATE_MOVIE",
                  "title": "Deadpool & Wolverine",
                  "durationMinutes": 128,
                  "ageRating": 18,
                  "genre": "Action, Comedy",
                  "status": "NOW_SHOWING"
                }
              ]
            }
            ```
            """);

        when(auditoriumService.create(eq(1L), any())).thenReturn(
            AuditoriumResponse.builder()
                .id(3L)
                .name("Screen 3")
                .cinemaId(1L)
                .cinemaName("Cinex Landmark 81")
                .totalRows(10)
                .columnsPerRow(12)
                .seatCount(120)
                .active(true)
                .build()
        );

        when(movieService.create(any())).thenReturn(
            MovieResponse.builder()
                .id(9L)
                .title("Deadpool & Wolverine")
                .durationMinutes(128)
                .genre("Action, Comedy")
                .status(MovieStatus.NOW_SHOWING)
                .active(true)
                .build()
        );

        AdminAiService adminAiService = new AdminAiService(
            nvidiaAiService, cinemaRepository, cinemaService,
            auditoriumRepository, auditoriumService,
            movieRepository, movieService, showtimeService,
            tmdbClient, youtubeService, new ObjectMapper()
        );

        AdminAiResponse response = adminAiService.chat("Tạo phòng Screen 3 và thêm phim Deadpool & Wolverine", List.of());

        assertThat(response.dataChanged()).isTrue();
        assertThat(response.actions()).hasSize(2);
        assertThat(response.actions().get(0).success()).isTrue();
        assertThat(response.actions().get(0).title()).contains("Screen 3");
        assertThat(response.actions().get(1).success()).isTrue();
        assertThat(response.actions().get(1).title()).contains("Deadpool & Wolverine");
        assertThat(response.reply()).doesNotContain("```json");
    }

    @Test
    void movieServiceRejectsDuplicateTitleIgnoringCaseAndExtraSpaces() {
        MovieRepository movieRepository = mock(MovieRepository.class);
        ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
        ShowtimeSeatRepository showtimeSeatRepository = mock(ShowtimeSeatRepository.class);
        BookingRepository bookingRepository = mock(BookingRepository.class);

        MovieService movieService = new MovieService(
            movieRepository,
            new YoutubeEmbedUrlNormalizer(),
            showtimeRepository,
            showtimeSeatRepository,
            bookingRepository
        );

        Movie existing = Movie.builder()
            .title("Lật Mặt 7")
            .status(MovieStatus.NOW_SHOWING)
            .active(true)
            .build();
        existing.setId(11L);
        when(movieRepository.findByActiveTrue()).thenReturn(List.of(existing));

        MovieRequest duplicateReq = new MovieRequest();
        duplicateReq.setTitle("  lật   mặt 7 ");
        duplicateReq.setStatus(MovieStatus.NOW_SHOWING);

        assertThatThrownBy(() -> movieService.create(duplicateReq))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("đã tồn tại");
    }
}
