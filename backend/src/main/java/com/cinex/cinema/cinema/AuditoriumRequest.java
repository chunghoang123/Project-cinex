package com.cinex.cinema.cinema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuditoriumRequest {

    @NotBlank(message = "Tên phòng chiếu không được để trống")
    @Size(max = 50)
    private String name;

    @NotNull(message = "Số hàng không được để trống")
    @Min(value = 1, message = "Số hàng phải lớn hơn 0")
    @Max(value = 30, message = "Số hàng tối đa 30")
    private Integer totalRows;

    @NotNull(message = "Số cột không được để trống")
    @Min(value = 1, message = "Số cột phải lớn hơn 0")
    @Max(value = 30, message = "Số cột tối đa 30")
    private Integer columnsPerRow;
}
