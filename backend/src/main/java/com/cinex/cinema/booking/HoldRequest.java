package com.cinex.cinema.booking;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class HoldRequest {

    @NotNull(message = "Suất chiếu không được để trống")
    private Long showtimeId;

    @NotEmpty(message = "Vui lòng chọn ít nhất một ghế")
    private List<Long> seatIds;

    /** Tiền bắp nước (không bắt buộc). */
    @DecimalMin(value = "0", message = "Tiền bắp nước không hợp lệ")
    private BigDecimal snackAmount;

    private String snackNote;
}
