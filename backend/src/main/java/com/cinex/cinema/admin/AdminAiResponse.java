package com.cinex.cinema.admin;

import java.util.List;

/**
 * Phản hồi từ Trợ lý AI trang Admin: câu trả lời hội thoại + danh sách thao tác đã thực thi.
 */
public record AdminAiResponse(
    String reply,
    String model,
    List<AdminAiActionResult> actions,
    boolean dataChanged
) {
}
