package com.cinex.cinema.admin;

/**
 * Kết quả thực thi một thao tác quản trị do Trợ lý AI thực hiện.
 */
public record AdminAiActionResult(
    String type,
    boolean success,
    String title,
    String detail,
    Long entityId
) {
}
