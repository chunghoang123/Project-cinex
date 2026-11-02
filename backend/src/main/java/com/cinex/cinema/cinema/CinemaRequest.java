package com.cinex.cinema.cinema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CinemaRequest {

    @NotBlank(message = "Tên rạp không được để trống")
    @Size(max = 100)
    private String name;

    @Size(max = 200)
    private String address;

    @Size(max = 20)
    private String phone;
}
