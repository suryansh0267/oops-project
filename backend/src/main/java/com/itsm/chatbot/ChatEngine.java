package com.itsm.chatbot;

import com.itsm.strategy.NamedStrategy;

/** Pattern: Strategy. The engine can enrich extraction/phrasing but cannot execute actions. */
public interface ChatEngine extends NamedStrategy {
    DraftExtraction extract(String sanitizedMessage);
    String phrase(String template, String sanitizedMessage);
    record DraftExtraction(String title, String category, String impact, String urgency, Integer affectedUsers) { }
}
