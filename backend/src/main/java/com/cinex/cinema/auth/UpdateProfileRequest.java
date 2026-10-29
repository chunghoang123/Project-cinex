package com.cinex.cinema.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Dữ liệu khách hàng tự cập nhật trong trang hồ sơ.
 * Các trường không gửi lên (null) sẽ được giữ nguyên, trừ họ tên và email là bắt buộc.
 */
@Data
public class UpdateProfileRequest {

    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    /** Chuỗi rỗng nghĩa là xoá số điện thoại đã lưu. */
    @Pattern(regexp = "^$|^[0-9+\\s.-]{8,15}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Size(max = 500, message = "Đường dẫn ảnh tối đa 500 ký tự")
    private String avatarUrl;
}
