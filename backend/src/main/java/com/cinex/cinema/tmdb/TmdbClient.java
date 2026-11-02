package com.cinex.cinema.tmdb;

import com.cinex.cinema.tmdb.TmdbDtos.TmdbGenre;
import com.cinex.cinema.tmdb.TmdbDtos.TmdbMovie;
import com.cinex.cinema.tmdb.TmdbDtos.TmdbPage;
import com.cinex.cinema.tmdb.TmdbDtos.TmdbVideo;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Client gọi TMDB REST API v3 bằng RestClient.
 * Mọi request đều truyền api_key qua query param.
 */
@Service
public class TmdbClient {

    /** Base URL của thư viện ảnh poster/backdrop của TMDB */
    public static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p";

    private final RestClient restClient;
    private final TmdbProperties props;

    public TmdbClient(RestClient.Builder restClientBuilder, TmdbProperties props) {
        this.props = props;
        this.restClient = restClientBuilder.build();
    }

    /** Phim đang chiếu (theo region cấu hình, mặc định VN). */
    public TmdbPage<TmdbMovie> nowPlaying(int page) {
        return fetch("/movie/now_playing", page);
    }

    /** Phim sắp ra mắt. */
    public TmdbPage<TmdbMovie> upcoming(int page) {
        return fetch("/movie/upcoming", page);
    }

    /** Tìm kiếm phim theo từ khoá. */
    public TmdbPage<TmdbMovie> search(String query, int page) {
        URI uri = buildUri("/search/movie")
            .queryParam("query", query)
            .queryParam("page", page)
            .build().encode().toUri();
        return execute(uri, moviePageType());
    }

    /** Chi tiết một phim (bổ sung runtime...). */
    public TmdbMovie movieDetail(long tmdbId) {
        URI uri = buildUri("/movie/" + tmdbId).build().encode().toUri();
        return execute(uri, movieType());
    }

    /** Danh sách video (trailer) của phim. */
    public List<TmdbVideo> movieVideos(long tmdbId) {
        URI uri = buildUri("/movie/" + tmdbId + "/videos")
            // YouTube trailer tiếng Anh đầy đủ hơn tiếng Việt nên lấy bản gốc
            .queryParam("language", "en-US")
            .build().encode().toUri();
        TmdbPage<TmdbVideo> page = execute(uri, new ParameterizedTypeReference<>() {
        });
        return page.results() == null ? List.of() : page.results();
    }

    /** Danh sách thể loại phim (ám xá tên thể loại theo language cấu hình). */
    public List<TmdbGenre> movieGenres() {
        URI uri = buildUri("/genre/movie/list").build().encode().toUri();
        TmdbGenreResponse response = execute(uri, new ParameterizedTypeReference<>() {
        });
        return response.genres() == null ? List.of() : response.genres();
    }

    /** URL đầy đủ của poster, null nếu TMDB không có ảnh. */
    public String posterUrl(String posterPath) {
        return imagePath(posterPath, "w500");
    }

    /** URL đầy đủ của backdrop (ảnh bìa ngang), null nếu TMDB không có ảnh. */
    public String backdropUrl(String backdropPath) {
        return imagePath(backdropPath, "w780");
    }

    private TmdbPage<TmdbMovie> fetch(String path, int page) {
        URI uri = buildUri(path)
            .queryParam("page", page)
            .build().encode().toUri();
        return execute(uri, moviePageType());
    }

    private UriComponentsBuilder buildUri(String path) {
        return UriComponentsBuilder.fromHttpUrl(props.getBaseUrl())
            .path(path)
            .queryParam("api_key", props.getApiKey())
            .queryParam("language", props.getLanguage());
    }

    /** Phim đang chiếu cần thêm region để hiển thị theo ngày công chiếu tại VN. */
    private record TmdbGenreResponse(List<TmdbGenre> genres) {
    }

    private String imagePath(String path, String size) {
        return (path == null || path.isBlank())
            ? null
            : IMAGE_BASE_URL + "/" + size + path;
    }

    private ParameterizedTypeReference<TmdbPage<TmdbMovie>> moviePageType() {
        return new ParameterizedTypeReference<>() {
        };
    }

    private ParameterizedTypeReference<TmdbMovie> movieType() {
        return new ParameterizedTypeReference<>() {
        };
    }

    private <T> T execute(URI uri, ParameterizedTypeReference<T> type) {
        return restClient.get()
            .uri(uri)
            .retrieve()
            .body(type);
    }
}
