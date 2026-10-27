package com.cinex.cinema.booking;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class HoldRequest {

    @NotNull(message = "Suất chiếu không được để trống")
    private Long showtimeId;

    @NotEmpty(message = "Vui lòng chọn ít nhất một ghế")
    private List<Long> seatIds;
}
