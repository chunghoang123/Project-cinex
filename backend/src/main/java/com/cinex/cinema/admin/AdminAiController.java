package com.cinex.cinema.admin;

import com.cinex.cinema.ai.AiChatRequest;
import com.cinex.cinema.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint Trợ lý AI dành riêng cho Admin (`/api/admin/ai/chat`).
 * Hỗ trợ tạo phòng chiếu, thêm phim, tạo/cập nhật suất chiếu và tự động xếp lịch 10 ngày.
 */
@RestController
@RequestMapping("/api/admin/ai")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiController {

    private final AdminAiService adminAiService;

    public AdminAiController(AdminAiService adminAiService) {
        this.adminAiService = adminAiService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AdminAiResponse>> chat(@Valid @RequestBody AiChatRequest request) {
        AdminAiResponse response = adminAiService.chat(request.message(), request.history());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
