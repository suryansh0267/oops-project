package com.itsm.chatbot;

import java.util.Map;

/** Injectable HTTP seam; unit tests can replace it with a deterministic fake. */
public interface LlmTransport {
    String post(String url, Map<String, String> headers, String json) throws Exception;
}
