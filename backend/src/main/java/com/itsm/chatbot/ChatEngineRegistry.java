package com.itsm.chatbot;

import com.itsm.strategy.StrategyRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.List;

/** Pattern: Strategy registry, reusing the project's generic registry. */
@Component
public class ChatEngineRegistry extends StrategyRegistry<ChatEngine> {
    public ChatEngineRegistry(List<ChatEngine> engines, @Value("${app.chatbot.engine:rule}") String defaultKey) {
        super(engines, defaultKey);
    }
    public ChatEngine engine() { return active(); }
}
