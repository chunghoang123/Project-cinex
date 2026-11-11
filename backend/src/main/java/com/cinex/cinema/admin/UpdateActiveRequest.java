package com.cinex.cinema.admin;

import jakarta.validation.constraints.NotNull;

/** Bật/tắt hoạt động (bảo trì) cho rạp hoặc phòng chiếu. */
public record UpdateActiveRequest(
    @NotNull(message = "Trạng thái không được để trống")
    Boolean active
) {
}
