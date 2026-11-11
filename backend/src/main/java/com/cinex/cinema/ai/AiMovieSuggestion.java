package com.cinex.cinema.ai;

/** Phim thật trong hệ thống được gắn kèm phản hồi để hiển thị poster trong box chat. */
public record AiMovieSuggestion(
    Long movieId,
    String title,
    String posterUrl,
    String genre
) {
}
