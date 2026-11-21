package com.itsm.strategy;

import com.itsm.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic registry of interchangeable strategies (Strategy pattern + generics).
 * Spring injects every implementation; the admin picks the active one at runtime.
 */
@Slf4j
public abstract class StrategyRegistry<T extends NamedStrategy> {

    private final Map<String, T> strategies = new LinkedHashMap<>();
    private volatile String activeKey;

    protected StrategyRegistry(List<T> implementations, String defaultKey) {
        for (T impl : implementations) {
            strategies.put(impl.getKey(), impl);
        }
        if (strategies.isEmpty()) {
            throw new IllegalStateException(getClass().getSimpleName() + " has no strategies");
        }
        this.activeKey = strategies.containsKey(defaultKey) ? defaultKey : strategies.keySet().iterator().next();
        log.info("{} ready with {} (active: {})", getClass().getSimpleName(), strategies.keySet(), activeKey);
    }

    public void setActiveStrategy(String key) {
        if (key == null || !strategies.containsKey(key)) {
            throw new BadRequestException("Unknown strategy '" + key + "'. Available: " + strategies.keySet());
        }
        this.activeKey = key;
        log.info("{} active strategy changed to {}", getClass().getSimpleName(), key);
    }

    public String getActiveStrategyKey() {
        return activeKey;
    }

    protected T active() {
        return strategies.get(activeKey);
    }

    public List<Map<String, Object>> getAvailableStrategies() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (T s : strategies.values()) {
            list.add(Map.of(
                    "key", s.getKey(),
                    "name", s.getDisplayName(),
                    "description", s.getDescription(),
                    "isActive", s.getKey().equals(activeKey)));
        }
        return list;
    }
}
