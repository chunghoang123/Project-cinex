package com.cinex.cinema.tmdb;

import com.cinex.cinema.movie.Movie;
import com.cinex.cinema.movie.MovieRepository;
import com.cinex.cinema.movie.MovieStatus;
import com.cinex.cinema.tmdb.TmdbDtos.TmdbGenre;
import com.cinex.cinema.tmdb.TmdbDtos.TmdbMovie;
import com.cinex.cinema.tmdb.TmdbDtos.TmdbPage;
import com.cinex.cinema.tmdb.TmdbDtos.TmdbVideo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;import java.util.stream.Collectors;

/**
 * Đồng bộ dữ liệu phim từ TMDB vào bảng movies của hệ thống:
 * toàn bộ phim đang chiếu + sắp ra mắt tại VN, trailer YouTube, poster.
 *
 * Phim trùng (đã có trong DB) được cập nhật theo tmdbId, không tạo bản ghi mới.
 */
@Slf4j
@Service
@Transactional
public class TmdbSyncService {

    private final TmdbClient tmdbClient;
    private final MovieRepository movieRepository;

    public TmdbSyncService(TmdbClient tmdbClient, MovieRepository movieRepository) {
        this.tmdbClient = tmdbClient;
        this.movieRepository = movieRepository;
    }

    /** Kết quả của một lần đồng bộ, trả về cho admin biết số phim thêm/cập nhật. */
    public record TmdbSyncResult(int added, int updated, int total) {
    }

    /**
     * Đồng bộ phim đang chiếu và sắp ra mắt (mỗi danh sách lấy `pages` trang, mặc định 20 phim/trang).
     */
    public TmdbSyncResult syncNowPlayingAndUpcoming(int pages) {
        try {
            Map<Integer, String> genreNames = loadGenreNames();
            Set<TmdbMovie> tmdbMovies = collectNowPlayingAndUpcoming(pages);

            int added = 0;
            int updated = 0;
            for (TmdbMovie tmdbMovie : tmdbMovies) {
                boolean existed = movieRepository.findByTmdbId(tmdbMovie.id()).isPresent();
                upsert(tmdbMovie, genreNames);
                if (existed) {
                    updated++;
                } else {
                    added++;
                }
            }
            log.info("Đồng bộ TMDB xong: thêm {} phim, cập nhật {} phim", added, updated);
            return new TmdbSyncResult(added, updated, tmdbMovies.size());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi đồng bộ dữ liệu phim từ TMDB", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không lấy được dữ liệu phim từ TMDB, thử lại sau");
        }
    }

    /** Gộp danh sách đang chiếu + sắp ra mắt, loại trùng theo tmdbId (LinkedHashSet giữ thứ tự). */
    private LinkedHashSet<TmdbMovie> collectNowPlayingAndUpcoming(int pages) {
        LinkedHashSet<TmdbMovie> movies = new LinkedHashSet<>();
        for (int page = 1; page <= pages; page++) {
            movies.addAll(requireResults(tmdbClient.nowPlaying(page)));
            movies.addAll(requireResults(tmdbClient.upcoming(page)));
        }
        return movies;
    }

    private List<TmdbMovie> requireResults(TmdbPage<TmdbMovie> page) {
        if (page == null || page.results() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "TMDB trả về dữ liệu rỗng");
        }
        return page.results();
    }

    /** Thêm mới hoặc cập nhật một phim TMDB vào DB. */
    private void upsert(TmdbMovie tmdbMovie, Map<Integer, String> genreNames) {
        Movie movie = movieRepository.findByTmdbId(tmdbMovie.id()).orElseGet(Movie::new);

        movie.setTmdbId(tmdbMovie.id());
        movie.setTitle(tmdbMovie.title() != null ? tmdbMovie.title() : tmdbMovie.originalTitle());
        movie.setDescription(tmdbMovie.overview());
        movie.setPosterUrl(tmdbClient.posterUrl(tmdbMovie.posterPath()));
        movie.setReleaseDate(tmdbMovie.releaseDate());
        movie.setGenre(joinGenres(tmdbMovie, genreNames));
        movie.setActive(true);

        // Phim ra mắt trước ngày hiện tại coi như đã ở rạp, ngược lại là sắp chiếu
        LocalDate today = LocalDate.now();
        movie.setStatus(tmdbMovie.releaseDate() == null || !tmdbMovie.releaseDate().isAfter(today)
            ? MovieStatus.NOW_SHOWING
            : MovieStatus.COMING_SOON);

        // Thời lượng và trailer chỉ có khi gọi API chi tiết từng phim
        TmdbMovie detail = tmdbClient.movieDetail(tmdbMovie.id());
        if (detail.runtime() != null && detail.runtime() > 0) {
            movie.setDurationMinutes(detail.runtime());
        }
        String trailerUrl = findTrailerUrl(tmdbMovie.id());
        if (trailerUrl != null) {
            movie.setTrailerUrl(trailerUrl);
        }

        movieRepository.save(movie);
    }

    /** Lấy trailer YouTube đầu tiên (ưu tiên Trailer > Teaser > video khác). */
    private String findTrailerUrl(long tmdbId) {
        List<TmdbVideo> videos = tmdbClient.movieVideos(tmdbId);
        Comparator<TmdbVideo> byImportance = Comparator
            .comparingInt((TmdbVideo v) -> "Trailer".equalsIgnoreCase(v.type()) ? 0 : 1)
            .thenComparingInt(v -> "Teaser".equalsIgnoreCase(v.type()) ? 0 : 1);
        return videos.stream()
            .filter(v -> "YouTube".equalsIgnoreCase(v.site()) && v.key() != null)
            .min(byImportance)
            .map(v -> "https://www.youtube.com/watch?v=" + v.key())
            .orElse(null);
    }

    /** Ghép tối đa 3 tên thể loại tiếng Việt thành chuỗi "Hành động, Kinh dị". */
    private String joinGenres(TmdbMovie tmdbMovie, Map<Integer, String> genreNames) {
        if (tmdbMovie.genreIds() == null) {
            return null;
        }
        return tmdbMovie.genreIds().stream()
            .map(genreNames::get)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .limit(3)
            .collect(Collectors.joining(", "));
    }

    private Map<Integer, String> loadGenreNames() {
        List<TmdbGenre> genres = tmdbClient.movieGenres();
        return genres.stream()
            .filter(g -> g.id() != null && g.name() != null)
            .collect(Collectors.toMap(TmdbGenre::id, TmdbGenre::name, (a, b) -> a));
    }
}
