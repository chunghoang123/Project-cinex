package com.cinex.cinema.showtime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ShowtimeRequest {

    @NotNull(message = "Phim không được để trống")
    private Long movieId;

    @NotNull(message = "Phòng chiếu không được để trống")
    private Long auditoriumId;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalDateTime startTime;

    @NotNull(message = "Giá vé không được để trống")
    @DecimalMin(value = "0", message = "Giá vé không hợp lệ")
    private BigDecimal basePrice;
}
