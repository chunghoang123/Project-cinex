package com.cinex.cinema.trailer;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình kết nối YouTube Data API v3 để lấy trailer.
 * Giá trị đọc từ application.properties (tiến tố youtube.*),
 * API key chỉ nhập qua biến môi trường YOUTUBE_API_KEY.
 */
@Data
@ConfigurationProperties("youtube")
public class YoutubeProperties {

    /**
     * API key cá nhân lấy tại Google Cloud Console
     * (APIs & Services > Credentials) cho YouTube Data API v3.
     */
    private String apiKey = "";

    /** Base URL của YouTube Data API v3 */
    private String baseUrl = "https://www.googleapis.com/youtube/v3";

    /**
     * Tìm trailer YouTube theo từ khoá (tên phim).
     * Chỉ tìm video (không trộn kênh/playlist), giới hạn số lượng mặc định.
     */
    private boolean enabled = true;
}
