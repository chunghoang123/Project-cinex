package com.cinex.cinema.tmdb;

import com.cinex.cinema.common.ApiResponse;
import com.cinex.cinema.tmdb.TmdbDtos.TmdbMovie;
import com.cinex.cinema.tmdb.TmdbDtos.TmdbPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * API proxy dữ liệu phim TMDB:
 * - GET: xem trước phim đang chiếu / sắp ra mắt / tìm kiếm (không cần đăng nhập)
 * - POST /sync: chỉ ADMIN, đẩy dữ liệu TMDB vào bảng movies của hệ thống
 */
@Slf4j
@RestController
@RequestMapping("/api/tmdb")
public class TmdbController {

    private final TmdbClient tmdbClient;
    private final TmdbSyncService tmdbSyncService;

    public TmdbController(TmdbClient tmdbClient, TmdbSyncService tmdbSyncService) {
        this.tmdbClient = tmdbClient;
        this.tmdbSyncService = tmdbSyncService;
    }

    @GetMapping("/now-playing")
    public ResponseEntity<ApiResponse<TmdbPage<TmdbMovie>>> nowPlaying(
            @RequestParam(defaultValue = "1") int page) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(tmdbClient.nowPlaying(page)));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không lấy được dữ liệu phim từ TMDB, thử lại sau");
        }
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<TmdbPage<TmdbMovie>>> upcoming(
            @RequestParam(defaultValue = "1") int page) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(tmdbClient.upcoming(page)));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không lấy được dữ liệu phim từ TMDB, thử lại sau");
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<TmdbPage<TmdbMovie>>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(tmdbClient.search(query, page)));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không lấy được dữ liệu phim từ TMDB, thử lại sau");
        }
    }

    @GetMapping("/movies/{tmdbId}")
    public ResponseEntity<ApiResponse<TmdbMovie>> movieDetail(@PathVariable long tmdbId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(tmdbClient.movieDetail(tmdbId)));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không lấy được dữ liệu phim từ TMDB, thử lại sau");
        }
    }

    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TmdbSyncService.TmdbSyncResult>> sync(
            @RequestParam(defaultValue = "1") int pages) {
        return ResponseEntity.ok(ApiResponse.ok("Đồng bộ dữ liệu phim từ TMDB thành công",
            tmdbSyncService.syncNowPlayingAndUpcoming(pages)));
    }
}
