package com.itsm.chatbot;

public class ChatRateLimitException extends RuntimeException {
    public ChatRateLimitException() { super("You're sending messages quickly. Please wait a minute and try again."); }
}
