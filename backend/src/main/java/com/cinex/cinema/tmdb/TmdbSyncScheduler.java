package com.cinex.cinema.tmdb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tự động đồng bộ dữ liệu phim TMDB theo lịch (mặc định 03:00 sáng mỗi ngày).
 * Bật bằng property tmdb.auto-sync.enabled=true trong application properties.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "tmdb.auto-sync.enabled", havingValue = "true")
public class TmdbSyncScheduler {

    private final TmdbSyncService tmdbSyncService;

    public TmdbSyncScheduler(TmdbSyncService tmdbSyncService) {
        this.tmdbSyncService = tmdbSyncService;
    }

    @Scheduled(cron = "${tmdb.sync-cron:0 0 3 * * *}")
    public void syncDaily() {
        log.info("Chạy lịch đồng bộ dữ liệu phim từ TMDB...");
        tmdbSyncService.syncNowPlayingAndUpcoming(1);
    }
}
