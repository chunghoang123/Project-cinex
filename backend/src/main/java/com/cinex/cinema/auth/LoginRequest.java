package com.cinex.cinema.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Tên đăng nhập hoặc email không được để trống")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}