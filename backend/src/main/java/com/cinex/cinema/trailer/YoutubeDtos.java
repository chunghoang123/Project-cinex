package com.cinex.cinema.trailer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO ánh xạ dữ liệu trả về từ YouTube Data API v3
 * (search.list, videos.list, channels.list...).
 * Chỉ map những field cần dùng, field lạ bị bỏ qua.
 */
public final class YoutubeDtos {

    private YoutubeDtos() {
    }

    /** Phần tử kết quả của search.list. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record YoutubeSearchResult(
        String kind,
        @JsonProperty("etag") String etag,
        List<SearchItem> items,
        PageInfo pageInfo,
        @JsonProperty("nextPageToken") String nextPageToken
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SearchItem(
        Id id,
        Snippet snippet
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Id(
        String kind,
        @JsonProperty("videoId") String videoId,
        String channelId,
        String playlistId
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Snippet(
        String title,
        String description,
        String channelTitle,
        @JsonProperty("channelId") String channelId,
        Thumbnails thumbnails,
        @JsonProperty("publishedAt") String publishedAt
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Thumbnails(
        Thumbnail default_,
        Thumbnail medium,
        Thumbnail high
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Thumbnail(
        String url
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PageInfo(
        @JsonProperty("totalResults") int totalResults,
        @JsonProperty("resultsPerPage") int resultsPerPage
    ) {
    }

    /** Kết quả videos.list (một hoặc nhiều video). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record YoutubeVideoResult(
        String kind,
        @JsonProperty("etag") String etag,
        List<VideoItem> items,
        PageInfo pageInfo
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VideoItem(
        String id,
        Snippet snippet,
        Statistics statistics
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Statistics(
        @JsonProperty("viewCount") String viewCount,
        @JsonProperty("likeCount") String likeCount
    ) {
    }

    /** Response cho frontend: gọn, có sẵn link embed + watch. */
    public record YoutubeTrailer(
        String videoId,
        String title,
        String channelTitle,
        String thumbnailUrl,
        String embedUrl,
        String watchUrl,
        String duration
    ) {
    }
}
