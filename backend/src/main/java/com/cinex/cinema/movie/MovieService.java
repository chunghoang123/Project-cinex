package com.cinex.cinema.movie;

import com.cinex.cinema.booking.BookingRepository;
import com.cinex.cinema.booking.BookingStatus;
import com.cinex.cinema.showtime.Showtime;
import com.cinex.cinema.showtime.ShowtimeRepository;
import com.cinex.cinema.showtime.ShowtimeSeatRepository;
import com.cinex.cinema.showtime.ShowtimeStatus;
import com.cinex.cinema.trailer.YoutubeEmbedUrlNormalizer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class MovieService {

    private final MovieRepository movieRepository;
    private final YoutubeEmbedUrlNormalizer youtubeEmbedUrlNormalizer;

    public MovieService(MovieRepository movieRepository,
                        YoutubeEmbedUrlNormalizer youtubeEmbedUrlNormalizer) {
        this.movieRepository = movieRepository;
        this.youtubeEmbedUrlNormalizer = youtubeEmbedUrlNormalizer;
        this.showtimeRepository = showtimeRepository;
        this.showtimeSeatRepository = showtimeSeatRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<MovieResponse> findAll(MovieStatus status) {
        List<Movie> movies = (status != null)
            ? movieRepository.findByStatusAndActiveTrue(status)
            : movieRepository.findByActiveTrue();
        return movies.stream().map(this::toResponse).toList();
    }

    public List<MovieResponse> findAllAdmin() {
        return movieRepository.findByActiveTrue().stream()
            .map(this::toResponse)
            .toList();
    }

    public MovieResponse findById(Long id) {
        Movie movie = movieRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_MOVIE_NOT_FOUND));
        return toResponse(movie);
    }

    public MovieResponse create(MovieRequest request) {
        Movie movie = Movie.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .posterUrl(request.getPosterUrl())
            // Chuẩn hoá trailer YouTube về embed để nhúng iframe (tránh lỗi "đã từ chối kết nối")
            .trailerUrl(normalizeTrailer(request.getTrailerUrl()))
            .durationMinutes(request.getDurationMinutes())
            .ageRating(request.getAgeRating())
            .genre(request.getGenre())
            .releaseDate(request.getReleaseDate())
            .status(request.getStatus())
            .active(true)
            .build();
        return toResponse(movieRepository.save(movie));
    }

    public MovieResponse update(Long id, MovieRequest request) {
        Movie movie = movieRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_MOVIE_NOT_FOUND));
        String normalizedTitle = normalizeTitle(request.getTitle());
        ensureUniqueTitle(normalizedTitle, id);

        movie.setTitle(normalizedTitle);
        movie.setDescription(request.getDescription());
        movie.setPosterUrl(request.getPosterUrl());
        // Chuẩn hoá trailer YouTube về embed để nhúng iframe (tránh lỗi "đã từ chối kết nối")
        movie.setTrailerUrl(normalizeTrailer(request.getTrailerUrl()));
        movie.setDurationMinutes(request.getDurationMinutes());
        movie.setAgeRating(request.getAgeRating());
        movie.setGenre(request.getGenre());
        movie.setReleaseDate(request.getReleaseDate());
        movie.setStatus(request.getStatus());
        return toResponse(movieRepository.save(movie));
    }

    public void delete(Long id) {
        Movie movie = movieRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_MOVIE_NOT_FOUND));

        List<Showtime> showtimes = showtimeRepository.findByMovieId(id);
        LocalDateTime now = LocalDateTime.now();
        boolean hasHistoricalBookings = false;

        for (Showtime showtime : showtimes) {
            boolean hasActiveBooking = bookingRepository.existsByShowtimeIdAndStatusIn(
                showtime.getId(),
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
            );
            if (hasActiveBooking && showtime.getEndTime() != null && showtime.getEndTime().isAfter(now)) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không thể xóa phim đang có suất chiếu đã bán vé hoặc đang giữ ghế"
                );
            }
            if (bookingRepository.existsByShowtimeId(showtime.getId())) {
                hasHistoricalBookings = true;
                showtime.setActive(false);
                if (showtime.getStatus() == ShowtimeStatus.ACTIVE) {
                    showtime.setStatus(ShowtimeStatus.CANCELLED);
                }
                showtimeRepository.save(showtime);
            } else {
                showtimeSeatRepository.deleteAll(showtimeSeatRepository.findByShowtimeId(showtime.getId()));
                showtimeRepository.delete(showtime);
            }
        }

        if (hasHistoricalBookings) {
            movie.setActive(false);
            movieRepository.save(movie);
        } else {
            movieRepository.delete(movie);
        }
    }

    private void ensureUniqueTitle(String title, Long excludeId) {
        String key = normalizeTitleKey(title);
        boolean duplicate = movieRepository.findByActiveTrue().stream()
            .filter(existing -> excludeId == null || !existing.getId().equals(excludeId))
            .anyMatch(existing -> normalizeTitleKey(existing.getTitle()).equals(key));
        if (duplicate) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Phim \"" + title + "\" đã tồn tại trong danh mục"
            );
        }
    }

    private static String normalizeTitle(String title) {
        return title == null ? "" : title.trim().replaceAll("\\s+", " ");
    }

    private static String normalizeTitleKey(String title) {
        return normalizeTitle(title).toLowerCase(Locale.ROOT);
    }

    private String normalizeTrailer(String trailerUrl) {
        return youtubeEmbedUrlNormalizer.toEmbed(trailerUrl);
    }

    private MovieResponse toResponse(Movie movie) {
        return MovieResponse.builder()
            .id(movie.getId())
            .tmdbId(movie.getTmdbId())
            .title(movie.getTitle())
            .description(movie.getDescription())
            .posterUrl(movie.getPosterUrl())
            .trailerUrl(movie.getTrailerUrl())
            .durationMinutes(movie.getDurationMinutes())
            .ageRating(movie.getAgeRating())
            .genre(movie.getGenre())
            .status(movie.getStatus())
            .releaseDate(movie.getReleaseDate())
            .active(movie.isActive())
            .createdAt(movie.getCreatedAt())
            .updatedAt(movie.getUpdatedAt())
            .build();
    }
}