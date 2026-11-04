package com.cinex.cinema.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class NvidiaAiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.nvidia.api-key}")
    private String apiKey;

    @Value("${app.nvidia.model}")
    private String model;

    public NvidiaAiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String describeImage(String base64Image, String prompt) {
        try {
            List<Map<String, Object>> content = List.of(
                Map.of("type", "text", "text", prompt),
                Map.of("type", "image_url", "image_url",
                    Map.of("url", "data:image/jpeg;base64," + base64Image))
            );

            Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                    Map.of("role", "user", "content", content)
                ),
                "temperature", 0.7,
                "max_tokens", 512
            );

            String json = objectMapper.writeValueAsString(body);

            ResponseEntity<String> response = restTemplate.postForEntity(
                "https://integrate.api.nvidia.com/v1/chat/completions",
                json,
                String.class
            );

            return extractContent(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("NVIDIA AI error: " + e.getMessage(), e);
        }
    }

    private String extractContent(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return node.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            return json;
        }
    }
}
