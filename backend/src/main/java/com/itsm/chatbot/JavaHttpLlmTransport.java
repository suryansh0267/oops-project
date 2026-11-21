package com.itsm.chatbot;

import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;

/** Pattern: Adapter. One HTTP attempt, 3s connect and 8s request deadlines. */
@Component
public class JavaHttpLlmTransport implements LlmTransport {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    @Override public String post(String url, Map<String, String> headers, String json) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        headers.forEach(request::header);
        HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("LLM HTTP error");
        return response.body();
    }
}
