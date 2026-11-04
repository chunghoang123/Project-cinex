package com.cinex.cinema.ai;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Giới hạn số lượt gọi AI theo từng IP để tránh lạm dụng endpoint công khai.
 * Bộ đếm nằm trong bộ nhớ, đủ dùng cho một instance backend.
 */
@Component
public class AiChatRateLimiter {

    private static final int MAX_REQUESTS = 20;
    private static final long WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();
    private static final int CLEANUP_THRESHOLD = 5_000;

    private final Map<String, Deque<Long>> requestsByClient = new ConcurrentHashMap<>();

    public void check(String clientKey) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = requestsByClient.computeIfAbsent(
            clientKey == null || clientKey.isBlank() ? "unknown" : clientKey,
            key -> new ConcurrentLinkedDeque<>()
        );

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MILLIS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_REQUESTS) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Bạn gửi tin nhắn quá nhanh, vui lòng thử lại sau ít phút");
            }
            timestamps.addLast(now);
        }

        if (requestsByClient.size() > CLEANUP_THRESHOLD) {
            requestsByClient.entrySet().removeIf(entry -> {
                Deque<Long> value = entry.getValue();
                synchronized (value) {
                    return value.isEmpty() || now - value.peekLast() > WINDOW_MILLIS;
                }
            });
        }
    }
}
