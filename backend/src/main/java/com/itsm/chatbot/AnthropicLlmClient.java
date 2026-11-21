package com.itsm.chatbot;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.util.Map;

/** Pattern: Adapter for Anthropic Messages API; never logs secrets or conversation text. */
@Component @RequiredArgsConstructor
public class AnthropicLlmClient implements LlmClient {
    private final LlmTransport http;
    private final ObjectMapper mapper;
    @Value("${app.chatbot.llm.anthropic-key:}") private String key;
    @Value("${app.chatbot.llm.anthropic-model:claude-haiku-5-5}") private String model;
    @Override public String provider() { return "anthropic"; }
    @Override public boolean configured() { return key != null && !key.isBlank(); }
    @Override public String generate(String prompt, String data) throws Exception {
        if (!configured()) throw new IllegalStateException("LLM is not configured");
        String body = mapper.writeValueAsString(Map.of("model", model, "max_tokens", 350, "system", prompt,
                "messages", new Object[]{Map.of("role", "user", "content", "<user_data>\n"
                        + data.replace("<", "‹").replace(">", "›") + "\n</user_data>")}));
        String response = http.post("https://api.anthropic.com/v1/messages",
                Map.of("x-api-key", key, "anthropic-version", "2023-06-01"), body);
        return mapper.readTree(response).path("content").path(0).path("text").asText("");
    }
}
