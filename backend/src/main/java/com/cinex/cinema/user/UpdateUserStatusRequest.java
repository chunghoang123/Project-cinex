package com.cinex.cinema.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private Boolean active;
}
