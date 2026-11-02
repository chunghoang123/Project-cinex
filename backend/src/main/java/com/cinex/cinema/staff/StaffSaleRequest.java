package com.cinex.cinema.staff;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StaffSaleRequest {

    @NotNull(message = "Suất chiếu không được để trống")
    private Long showtimeId;

    @NotEmpty(message = "Vui lòng chọn ít nhất một ghế")
    private List<Long> seatIds;

    private String customerName;
    private String customerPhone;

    private String paymentMethod;

    @DecimalMin(value = "0", message = "Tiền bắp nước không hợp lệ")
    private BigDecimal snackAmount;

    private String snackNote;
}
