package com.itsm.chatbot;

/** Pattern: Adapter. Provider responses contain data only, never executable commands. */
public interface LlmClient {
    String provider();
    boolean configured();
    String generate(String fixedPrompt, String sanitizedData) throws Exception;
}
