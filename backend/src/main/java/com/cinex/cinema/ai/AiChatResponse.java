package com.cinex.cinema.ai;

import java.util.List;

public record AiChatResponse(String reply, String model, List<AiMovieSuggestion> movies) {
    public AiChatResponse(String reply, String model) {
        this(reply, model, List.of());
    }
}
