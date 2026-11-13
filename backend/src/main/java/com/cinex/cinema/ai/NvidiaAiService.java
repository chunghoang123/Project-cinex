package com.cinex.cinema.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Gọi NVIDIA NIM qua backend để API key không bao giờ lộ ra frontend.
 * Không có câu trả lời dự phòng cục bộ: mọi câu trả lời đều do mô hình sinh ra.
 */
@Slf4j
@Service
public class NvidiaAiService {

    private static final String CHAT_COMPLETIONS_URL =
        "https://integrate.api.nvidia.com/v1/chat/completions";
    private static final String KEY_ROLE = "role";
    private static final String KEY_CONTENT = "content";
    private static final String KEY_MESSAGES = "messages";
    private static final String KEY_MODEL = "model";
    private static final String KEY_TEMPERATURE = "temperature";
    private static final String KEY_MAX_TOKENS = "max_tokens";
    private static final String KEY_STREAM = "stream";
    private static final String KEY_TYPE = "type";
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String ROLE_SYSTEM = "system";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.nvidia.api-key:}")
    private String apiKey;

    @Value("${app.nvidia.model:nvidia/nemotron-3.5-bolt-e4b}")
    private String model;

    @Value("${app.nvidia.vision-model:nvidia/nemotron-3.5-super-vl-120b-a12b}")
    private String visionModel;

    public NvidiaAiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String getModel() {
        return model;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** Hội thoại văn bản: system prompt + lịch sử gần nhất + câu hỏi mới. */
    public String chat(String systemPrompt, List<AiChatMessage> history, String message) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of(KEY_ROLE, ROLE_SYSTEM, KEY_CONTENT, systemPrompt));

        if (history != null) {
            List<AiChatMessage> recent = history.stream()
                .filter(item -> item != null && item.content() != null && !item.content().isBlank())
                .toList();
            int start = Math.max(0, recent.size() - 10);
            for (AiChatMessage item : recent.subList(start, recent.size())) {
                messages.add(Map.of(
                    KEY_ROLE, ROLE_ASSISTANT.equals(item.role()) ? ROLE_ASSISTANT : ROLE_USER,
                    KEY_CONTENT, item.content().trim()
                ));
            }
        }

        messages.add(Map.of(KEY_ROLE, ROLE_USER, KEY_CONTENT, message.trim()));

        Map<String, Object> body = Map.of(
            KEY_MODEL, model,
            KEY_MESSAGES, messages,
            KEY_TEMPERATURE, 0.55,
            "top_p", 0.9,
            KEY_MAX_TOKENS, 1024,
            KEY_STREAM, false
        );

        return complete(body, "trợ lý AI");
    }

    /** Mô tả ảnh bằng model vision của NVIDIA. */
    public String describeImage(String base64Image, String mimeType, String prompt) {
        List<Map<String, Object>> content = List.of(
            Map.of(KEY_TYPE, "text", "text", prompt),
            Map.of(KEY_TYPE, "image_url", "image_url",
                Map.of("url", "data:" + mimeType + ";base64," + base64Image))
        );

        Map<String, Object> body = Map.of(
            KEY_MODEL, visionModel,
            KEY_MESSAGES, List.of(Map.of(KEY_ROLE, ROLE_USER, KEY_CONTENT, content)),
            KEY_TEMPERATURE, 0.3,
            KEY_MAX_TOKENS, 700,
            KEY_STREAM, false
        );

        return complete(body, "phân tích hình ảnh");
    }

    private String complete(Map<String, Object> body, String feature) {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Trợ lý AI chưa được cấu hình (thiếu NVIDIA_API_KEY)");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(apiKey);

        try {
            String payload = objectMapper.writeValueAsString(body);
            ResponseEntity<String> response = restTemplate.exchange(
                CHAT_COMPLETIONS_URL,
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                String.class
            );
            return extractContent(response.getBody(), feature);
        } catch (HttpStatusCodeException ex) {
            int status = ex.getStatusCode().value();
            log.warn("NVIDIA {} thất bại: HTTP {} - {}", feature, status,
                ex.getResponseBodyAsString());
            if (status == 401 || status == 403) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "NVIDIA API key không hợp lệ hoặc không có quyền dùng model, liên hệ quản trị");
            }
            if (status == 429) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "NVIDIA đang giới hạn lượt gọi, vui lòng thử lại sau ít phút");
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "NVIDIA chưa phục vụ model hiện tại (" + status + "), thử lại ít phút nữa");
        } catch (ResourceAccessException ex) {
            log.warn("Không kết nối được NVIDIA cho {}", feature, ex);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Không kết nối được NVIDIA, vui lòng thử lại sau");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Lỗi khi gọi NVIDIA cho {}", feature, ex);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Không xử lý được phản hồi từ NVIDIA, vui lòng thử lại");
        }
    }

    private String extractContent(String json, String feature) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        JsonNode choices = node.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            log.warn("NVIDIA {} trả về JSON không có choices: {}", feature, json);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "NVIDIA trả về dữ liệu không hợp lệ, vui lòng thử lại");
        }
        String content = choices.get(0).path("message").path(KEY_CONTENT).asText("");
        if (content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "NVIDIA trả về câu trả lời trống, vui lòng thử lại");
        }
        return content.trim();
    }
}
