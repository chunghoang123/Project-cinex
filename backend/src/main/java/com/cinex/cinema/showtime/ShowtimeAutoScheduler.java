package com.cinex.cinema.showtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Bộ lập lịch tự động quản lý suất chiếu:
 * <ul>
 *   <li>Chu kỳ 5 ngày/lần (432.000.000 ms): tự động thêm và lấp đầy lịch chiếu cho 10 ngày tới.</li>
 *   <li>Mỗi 5 phút: tự động chuyển trạng thái các suất chiếu đã kết thúc sang COMPLETED.</li>
 * </ul>
 */
@Slf4j
@Component
public class ShowtimeAutoScheduler {

    private final ShowtimeService showtimeService;

    @Value("${app.showtime.auto-days:10}")
    private int autoDays;

    public ShowtimeAutoScheduler(ShowtimeService showtimeService) {
        this.showtimeService = showtimeService;
    }

    /**
     * Cứ 5 ngày (432_000_000 ms) tự động cập nhật và thêm suất chiếu cho 10 ngày tiếp theo.
     * Cũng chạy kiểm tra sau khi server khởi động 15 giây để đảm bảo luôn có đủ lịch 10 ngày.
     */
    @Scheduled(
        fixedRateString = "${app.showtime.refresh-interval-ms:432000000}",
        initialDelayString = "${app.showtime.initial-delay-ms:15000}"
    )
    public void autoExtendShowtimesEveryFiveDays() {
        try {
            ShowtimeService.AutoScheduleResult result = showtimeService.autoGenerateShowtimes(autoDays);
            log.info(
                "Tự động cập nhật lịch chiếu ({} ngày: {} -> {}): tạo mới {} suất, đóng {} suất đã kết thúc",
                result.daysAhead(), result.fromDate(), result.toDate(),
                result.createdCount(), result.completedCount()
            );
        } catch (Exception ex) {
            log.warn("Lỗi khi tự động cập nhật suất chiếu định kỳ", ex);
        }
    }

    /**
     * Mỗi 5 phút kiểm tra và chuyển các suất chiếu đã chiếu xong sang trạng thái COMPLETED.
     */
    @Scheduled(fixedRate = 300_000, initialDelay = 60_000)
    public void markCompletedShowtimes() {
        try {
            int completed = showtimeService.completeFinishedShowtimes();
            if (completed > 0) {
                log.info("Đã tự động chuyển {} suất chiếu kết thúc sang COMPLETED", completed);
            }
        } catch (Exception ex) {
            log.warn("Lỗi khi cập nhật trạng thái suất chiếu COMPLETED", ex);
        }
    }
}
