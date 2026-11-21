package com.itsm.chatbot;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.util.Map;

/** Pattern: Adapter for Gemini's generateContent API. Credentials stay in headers. */
@Component @RequiredArgsConstructor
public class GeminiLlmClient implements LlmClient {
    private final LlmTransport http;
    private final ObjectMapper mapper;
    @Value("${app.chatbot.llm.gemini-key:}") private String key;
    @Value("${app.chatbot.llm.gemini-model:gemini-3.8-flash}") private String model;
    @Override public String provider() { return "gemini"; }
    @Override public boolean configured() { return key != null && !key.isBlank(); }
    @Override public String generate(String prompt, String data) throws Exception {
        if (!configured()) throw new IllegalStateException("LLM is not configured");
        String body = mapper.writeValueAsString(Map.of("contents", new Object[]{Map.of("parts",
                new Object[]{Map.of("text", prompt + "\n<user_data>\n" + escapeBoundary(data) + "\n</user_data>")})}));
        String response = http.post("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent",
                Map.of("x-goog-api-key", key), body);
        return mapper.readTree(response).path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
    }
    private static String escapeBoundary(String text) { return text.replace("<", "‹").replace(">", "›"); }
}
