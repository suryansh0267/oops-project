package com.itsm.chatbot;

import java.util.List;

/** Role-safe response; no frustration metadata leaves the chat endpoint. */
public record ChatReply(String text, List<Card> cards, List<String> quickReplies, List<Action> actions) {
    public record Card(String title, String subtitle, String detail) { }
    public record Action(String type, Long incidentId, String label) { }
    public ChatReply(String text, List<String> replies) { this(text, List.of(), replies, List.of()); }
    public static ChatReply say(String text) { return new ChatReply(text, List.of()); }
}
