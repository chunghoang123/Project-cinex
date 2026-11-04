package com.cinex.cinema.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AiChatRequest(
    @NotBlank(message = "Vui lòng nhập tin nhắn")
    @Size(max = 2000, message = "Tin nhắn tối đa 2000 ký tự")
    String message,

    @Valid
    @Size(max = 20, message = "Chỉ gửi tối đa 20 lượt hội thoại gần nhất")
    List<AiChatMessage> history
) {
}
