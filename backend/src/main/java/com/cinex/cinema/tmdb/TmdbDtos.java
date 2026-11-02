package com.cinex.cinema.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO ánh xạ dữ liệu trả về từ TMDB API.
 * Chỉ map những field cần dùng, field lạ bị bỏ qua.
 */
public final class TmdbDtos {

    private TmdbDtos() {
    }

    /** Bản ghi phim trong danh sách (now_playing, upcoming, search) hoặc chi tiết phim. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbMovie(
        Long id,
        String title,
        @JsonProperty("original_title") String originalTitle,
        String overview,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("backdrop_path") String backdropPath,
        @JsonProperty("genre_ids") List<Integer> genreIds,
        @JsonProperty("release_date") LocalDate releaseDate,
        Integer runtime,
        Double popularity,
        @JsonProperty("vote_average") Double voteAverage
    ) {
    }

    /** Phản hồi phân trang của TMDB. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbPage<T>(
        Integer page,
        List<T> results,
        @JsonProperty("total_pages") Integer totalPages,
        @JsonProperty("total_results") Integer totalResults
    ) {
    }

    /** Segment video (trailer) của phim trên YouTube. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbVideo(
        String key,
        String name,
        String site,
        String type,
        Boolean official
    ) {
    }

    /** Thể loại phim (phản hồi của /genre/movie/list). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbGenre(
        Integer id,
        String name
    ) {
    }
}
