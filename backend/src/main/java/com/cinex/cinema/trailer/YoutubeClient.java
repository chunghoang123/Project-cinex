package com.cinex.cinema.trailer;

import com.cinex.cinema.trailer.YoutubeDtos.PageInfo;
import com.cinex.cinema.trailer.YoutubeDtos.SearchItem;
import com.cinex.cinema.trailer.YoutubeDtos.VideoItem;
import com.cinex.cinema.trailer.YoutubeDtos.YoutubeSearchResult;
import com.cinex.cinema.trailer.YoutubeDtos.YoutubeTrailer;
import com.cinex.cinema.trailer.YoutubeDtos.YoutubeVideoResult;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Client gọi YouTube Data API v3 để lấy trailer phim.
 * - search.list: tìm video theo từ khoá (tên phim)
 * - videos.list: lấy chi tiết một hoặc nhiều video (tối đa 50 id/lần gọi)
 * - channelsearch.list: không dùng, API này đã huỷ
 */
@Service
public class YoutubeClient {

    private final RestClient restClient;
    private final YoutubeProperties props;
    private final YoutubeEmbedUrlNormalizer normalizer;

    public YoutubeClient(RestClient.Builder restClientBuilder,
                         YoutubeProperties props,
                         YoutubeEmbedUrlNormalizer normalizer) {
        this.props = props;
        this.normalizer = normalizer;
        this.restClient = restClientBuilder.build();
    }

    public boolean isConfigured() {
        return props.getApiKey() != null && !props.getApiKey().isBlank();
    }

    /**
     * Tìm trailer YouTube theo từ khoá (tên phim).
     * Chỉ trả video (không trộn kênh/playlist), giới hạn số lượng theo maxResults.
     */
    public List<YoutubeTrailer> searchTrailers(String query, int maxResults) {
        ensureConfigured();

        int limit = Math.max(1, Math.min(50, maxResults));
        URI uri = UriComponentsBuilder.fromHttpUrl(props.getBaseUrl())
            .path("/search")
            .queryParam("part", "snippet")
            .queryParam("type", "video")
            .queryParam("videoCategoryId", "1")
            .queryParam("q", query)
            .queryParam("maxResults", limit)
            .queryParam("relevanceLanguage", "vi")
            .queryParam("key", props.getApiKey())
            .build()
            .encode()
            .toUri();

        YoutubeSearchResult result = execute(uri, new ParameterizedTypeReference<>() {
        });
        if (result == null || result.items() == null) {
            return List.of();
        }

        return result.items().stream()
            .map(this::toTrailer)
            .flatMap(Optional::stream)
            .toList();
    }

    /**
     * Lấy chi tiết nhiều video cùng lúc (videos.list, tối đa 50 id/lần).
     * Videos.list trả thông tin đầy đủ hơn search: tiêu đề, kênh, lượt xem...
     */
    public List<YoutubeTrailer> fetchVideos(List<String> videoIds) {
        ensureConfigured();

        if (videoIds == null || videoIds.isEmpty()) {
            return List.of();
        }

        Set<String> normalizedIds = videoIds.stream()
            .map(normalizer::extractVideoId)
            .flatMap(Optional::stream)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (normalizedIds.isEmpty()) {
            return List.of();
        }

        List<String> idsToFetch = normalizedIds.stream()
            .limit(50)
            .toList();

        URI uri = UriComponentsBuilder.fromHttpUrl(props.getBaseUrl())
            .path("/videos")
            .queryParam("part", "snippet,contentDetails,statistics")
            .queryParam("id", String.join(",", idsToFetch))
            .queryParam("key", props.getApiKey())
            .build()
            .encode()
            .toUri();

        YoutubeVideoResult result = execute(uri, new ParameterizedTypeReference<>() {
        });
        if (result == null || result.items() == null) {
            return List.of();
        }

        return result.items().stream()
            .map(this::toTrailer)
            .flatMap(Optional::stream)
            .toList();
    }

    /** Lấy trang thông tin video đơn lẻ (thông tin chi tiết cho một video). */
    public Optional<YoutubeTrailer> fetchVideo(String videoId) {
        List<YoutubeTrailer> trailers = fetchVideos(List.of(videoId));
        return trailers.isEmpty() ? Optional.empty() : Optional.of(trailers.get(0));
    }

    private Optional<YoutubeTrailer> toTrailer(SearchItem item) {
        if (item == null || item.id() == null || item.id().videoId() == null) {
            return Optional.empty();
        }
        String videoId = item.id().videoId();
        return Optional.of(toTrailer(videoId, item.snippet() != null ? item.snippet().title() : null,
            item.snippet() != null ? item.snippet().channelTitle() : null,
            pickThumbnail(item)));
    }

    private Optional<YoutubeTrailer> toTrailer(VideoItem item) {
        if (item == null || item.id() == null) {
            return Optional.empty();
        }
        String videoId = item.id();
        return Optional.of(toTrailer(videoId, item.snippet() != null ? item.snippet().title() : null,
            item.snippet() != null ? item.snippet().channelTitle() : null,
            pickThumbnailVideo(item)));
    }

    private YoutubeTrailer toTrailer(String videoId, String title, String channelTitle, String thumbnailUrl) {
        return new YoutubeTrailer(
            videoId,
            title != null ? title : "",
            channelTitle != null ? channelTitle : "",
            thumbnailUrl,
            normalizer.embedFromId(videoId),
            normalizer.watchFromId(videoId),
            ""
        );
    }

    private String pickThumbnail(SearchItem item) {
        try {
            var thumbnails = item.snippet().thumbnails();
            if (thumbnails.high() != null && thumbnails.high().url() != null) {
                return thumbnails.high().url();
            }
            if (thumbnails.medium() != null && thumbnails.medium().url() != null) {
                return thumbnails.medium().url();
            }
            if (thumbnails.default_() != null && thumbnails.default_().url() != null) {
                return thumbnails.default_().url();
            }
        } catch (Exception ignored) {
        }
        return "https://i" + (Math.abs(videoIdHash(item))) + ".ytimg.com/vi/" + item.id().videoId() + "/hqdefault.jpg";
    }

    private String pickThumbnailVideo(VideoItem item) {
        try {
            var thumbnails = item.snippet().thumbnails();
            if (thumbnails.high() != null && thumbnails.high().url() != null) {
                return thumbnails.high().url();
            }
            if (thumbnails.medium() != null && thumbnails.medium().url() != null) {
                return thumbnails.medium().url();
            }
            if (thumbnails.default_() != null && thumbnails.default_().url() != null) {
                return thumbnails.default_().url();
            }
        } catch (Exception ignored) {
        }
        return "https://i" + (Math.abs(videoIdHash(item))) + ".ytimg.com/vi/" + item.id() + "/hqdefault.jpg";
    }

    private int videoIdHash(Object item) {
        if (item instanceof SearchItem searchItem && searchItem.id() != null && searchItem.id().videoId() != null) {
            return searchItem.id().videoId().hashCode();
        }
        if (item instanceof VideoItem videoItem && videoItem.id() != null) {
            return videoItem.id().hashCode();
        }
        return 0;
    }

    public void ensureConfigured() {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "YouTube API chưa được cấu hình (thiếu YOUTUBE_API_KEY)");
        }
    }

    private <T> T execute(URI uri, ParameterizedTypeReference<T> type) {
        try {
            return restClient.get()
                .uri(uri)
                .retrieve()
                .body(type);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "YouTube Data API trả lỗi " + ex.getStatusCode().value() + " - kiểm tra API key hoặc giới hạn quota");
            }
            if (ex.getStatusCode().is5xxServerError()) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "YouTube Data API đang lỗi, vui lòng thử lại sau");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Không gọi được YouTube Data API");
        }
    }

    /**
     * Chuẩn hoá trailer ngay khi đưa vào DB: mọi URL YouTube luôn lưu dạng embed.
     * Giúp tránh bug "youtube.com đã từ chối kết nối" do dùng link watch/shorts.
     */
    public String normalize(String url) {
        return normalizer.toEmbed(url);
    }
}
