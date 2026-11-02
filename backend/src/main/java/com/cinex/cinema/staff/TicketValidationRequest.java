package com.cinex.cinema.staff;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TicketValidationRequest {

    @NotBlank(message = "Mã vé không được để trống")
    private String code;
}
