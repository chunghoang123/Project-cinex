package com.cinex.cinema.trailer;

import com.cinex.cinema.movie.Movie;
import com.cinex.cinema.movie.MovieRepository;
import com.cinex.cinema.trailer.YoutubeDtos.YoutubeTrailer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service thống nhất xử lý trailer:
 * - Tìm trailer trên YouTube cho admin
 * - Tự đồng bộ trailer candidate lên một movie
 */
@Service
public class YoutubeService {

    private final YoutubeClient youtubeClient;
    private final MovieRepository movieRepository;

    public YoutubeService(YoutubeClient youtubeClient, MovieRepository movieRepository) {
        this.youtubeClient = youtubeClient;
        this.movieRepository = movieRepository;
    }

    public List<YoutubeTrailer> search(String query, int maxResults) {
        return youtubeClient.searchTrailers(query, maxResults);
    }

    public List<YoutubeTrailer> fetchVideos(List<String> ids) {
        return youtubeClient.fetchVideos(ids);
    }

    /**
     * Đồng bộ trailer từ YouTube lên phim: tìm "tên phim + trailer" trên YouTube,
     * lấy video đầu tiên có thể nhúng embed và lưu vào movie.
     */
    @Transactional
    public Movie syncTrailer(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Phim không tồn tại"));

        List<YoutubeTrailer> trailers = youtubeClient.searchTrailers(
            movie.getTitle() + " official trailer", 5);
        if (trailers.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND,
                "Không tìm thấy trailer YouTube cho phim " + movie.getTitle());
        }

        YoutubeTrailer first = trailers.get(0);
        movie.setTrailerUrl(first.embedUrl());
        return movieRepository.save(movie);
    }

    /** Tìm trailer tự động cho phim khi tạo phim (dùng khi admin muốn auto fill). */
    public YoutubeTrailer findFirstTrailer(String movieTitle) {
        List<YoutubeTrailer> trailers = youtubeClient.searchTrailers(
            movieTitle + " official trailer", 3);
        return trailers.isEmpty() ? null : trailers.get(0);
    }
}
