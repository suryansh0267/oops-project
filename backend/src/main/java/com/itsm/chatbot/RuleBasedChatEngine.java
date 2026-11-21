package com.itsm.chatbot;

import org.springframework.stereotype.Component;

/** Pattern: Strategy. Default engine works fully offline. */
@Component
public class RuleBasedChatEngine implements ChatEngine {
    @Override public String getKey() { return "rule"; }
    @Override public String getDisplayName() { return "Local rule-based"; }
    @Override public String getDescription() { return "Deterministic, no keys or network"; }
    @Override public DraftExtraction extract(String text) { return new DraftExtraction(null, null, null, null, null); }
    @Override public String phrase(String template, String text) { return template; }
}
