package com.cinex.cinema.tmdb;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình kết nối TMDB API (The Movie Database).
 * Giá trị đọc từ application.properties (tiền tố tmdb.*).
 */
@Data
@ConfigurationProperties("tmdb")
public class TmdbProperties {

    /** API key cá nhân lấy từ trang https://www.themoviedb.org/settings/api */
    private String apiKey;

    /** Base URL của TMDB REST API */
    private String baseUrl = "https://api.themoviedb.org/3";

    /** Ngôn ngữ dữ liệu trả về (mô tả phim, tên thể loại...) */
    private String language = "vi-VN";

    /** Quốc gia dùng để lọc phim đang chiếu theo ngày công chiếu tại nước đó */
    private String region = "VN";

    /** Bật đồng bộ tự động theo lịch (cron) */
    private boolean autoSyncEnabled = false;

    /** Lịch cron cho auto-sync, mặc định 03:00 sáng mỗi ngày */
    private String syncCron = "0 0 3 * * *";
}
