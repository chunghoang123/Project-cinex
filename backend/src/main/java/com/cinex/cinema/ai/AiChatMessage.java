package com.cinex.cinema.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Một lượt hội thoại trước đó gửi kèm để AI giữ ngữ cảnh. */
public record AiChatMessage(
    @NotBlank
    @Pattern(regexp = "user|assistant", message = "role chỉ nhận user hoặc assistant")
    String role,

    @NotBlank
    @Size(max = 4000, message = "Nội dung tin nhắn tối đa 4000 ký tự")
    String content
) {
}
