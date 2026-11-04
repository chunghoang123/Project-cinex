package com.cinex.cinema.trailer;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chuẩn hóa các định dạng link YouTube về dạng embed để nhúng iframe
 * trên trình duyệt (link watch/shorts thường bị YouTube chặn nhúng).
 *
 * Hỗ trợ:
 * - https://www.youtube.com/watch?v=ID[&t=90s]
 * - https://youtu.be/ID
 * - https://www.youtube.com/embed/ID
 * - https://www.youtube.com/shorts/ID
 * - https://www.youtube.com/live/ID
 * - https://m.youtube.com/watch?v=ID
 */
@Component
public class YoutubeEmbedUrlNormalizer {

    private static final Pattern VIDEO_ID_PATTERN =
        Pattern.compile("[a-zA-Z0-9_-]{11}");

    private static final Pattern WATCH_PATTERN =
        Pattern.compile("(?:youtube\\.com/(?:watch\\?[^#]*v=|shorts/|embed/|live/|v/))([a-zA-Z0-9_-]{11})");
    private static final Pattern SHORT_PATTERN =
        Pattern.compile("youtu\\.be/([a-zA-Z0-9_-]{11})");

    public String toEmbed(String url) {
        return extractVideoId(url)
            .map(id -> "https://www.youtube.com/embed/" + id)
            .orElse(url);
    }

    public boolean isYoutubeUrl(String url) {
        return extractVideoId(url).isPresent();
    }

    /** Trích video id (11 ký tự) từ mọi dạng link, rỗng nếu không nhận diện được. */
    public Optional<String> extractVideoId(String url) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        String trimmed = url.trim();

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            Matcher watch = WATCH_PATTERN.matcher(trimmed);
            if (watch.find()) {
                return Optional.of(watch.group(1));
            }
            Matcher shortLink = SHORT_PATTERN.matcher(trimmed);
            if (shortLink.find()) {
                return Optional.of(shortLink.group(1));
            }
        }

        if (VIDEO_ID_PATTERN.matcher(trimmed).matches()) {
            return Optional.of(trimmed);
        }

        return Optional.empty();
    }

    /** Tạo link embed từ videoId thô. */
    public String embedFromId(String videoId) {
        return "https://www.youtube.com/embed/" + videoId.trim();
    }

    /** Tạo link watch từ videoId thô. */
    public String watchFromId(String videoId) {
        return "https://www.youtube.com/watch?v=" + videoId.trim();
    }
}
