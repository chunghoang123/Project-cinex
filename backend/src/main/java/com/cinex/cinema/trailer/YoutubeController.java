package com.cinex.cinema.trailer;

import com.cinex.cinema.common.ApiResponse;
import com.cinex.cinema.trailer.YoutubeDtos.YoutubeTrailer;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API proxy YouTube Data API v3 cho trailer.
 * - GET /api/youtube/trailers?q=... : tìm trailer (chỉ ADMIN)
 * - GET /api/youtube/videos?ids=... : chi tiết nhiều video (chỉ ADMIN)
 * - POST /api/youtube/sync/{movieId} : tự tìm và gắn trailer cho một phim (chỉ ADMIN)
 * Tất cả gọi qua backend để API key không lộ ra trình duyệt.
 */
@RestController
@RequestMapping("/api/youtube")
@PreAuthorize("hasRole('ADMIN')")
public class YoutubeController {

    private final YoutubeService youtubeService;

    public YoutubeController(YoutubeService youtubeService) {
        this.youtubeService = youtubeService;
    }

    @GetMapping("/trailers")
    public ResponseEntity<ApiResponse<List<YoutubeTrailer>>> searchTrailers(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "10") int maxResults) {
        return ResponseEntity.ok(ApiResponse.ok(youtubeService.search(query, maxResults)));
    }

    @GetMapping("/videos")
    public ResponseEntity<ApiResponse<List<YoutubeTrailer>>> fetchVideos(
            @RequestParam("ids") String ids) {
        List<String> split = List.of(ids.split(","));
        return ResponseEntity.ok(ApiResponse.ok(youtubeService.fetchVideos(split)));
    }

    @PostMapping("/sync/{movieId}")
    public ResponseEntity<ApiResponse<String>> syncTrailer(@PathVariable Long movieId) {
        youtubeService.syncTrailer(movieId);
        return ResponseEntity.ok(ApiResponse.ok("Đã đồng bộ trailer từ YouTube"));
    }
}
