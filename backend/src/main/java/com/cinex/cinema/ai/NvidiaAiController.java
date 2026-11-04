package com.cinex.cinema.ai;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class NvidiaAiController {

    private final NvidiaAiService nvidiaAiService;

    public NvidiaAiController(NvidiaAiService nvidiaAiService) {
        this.nvidiaAiService = nvidiaAiService;
    }

    @PostMapping("/describe-image")
    public ResponseEntity<Map<String, String>> describeImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "prompt", defaultValue = "Mô tả chi tiết hình ảnh này bằng tiếng Việt, 2-3 câu.") String prompt) {

        try {
            byte[] bytes = file.getBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String content = nvidiaAiService.describeImage(base64, prompt);
            return ResponseEntity.ok(Map.of("description", content));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }
}
