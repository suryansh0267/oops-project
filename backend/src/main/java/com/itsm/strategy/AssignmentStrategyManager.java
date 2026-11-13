package com.itsm.strategy;

import com.itsm.entity.Incident;
import com.itsm.entity.TechnicianProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class AssignmentStrategyManager {

    private final Map<String, AssignmentStrategy> strategies;
    private String activeStrategyKey = "skillBased";

    public AssignmentStrategyManager(Map<String, AssignmentStrategy> strategies) {
        this.strategies = strategies;
        log.info("Initialized AssignmentStrategyManager with strategies: {}", strategies.keySet());
    }

    public synchronized void setActiveStrategy(String strategyKey) {
        if (!strategies.containsKey(strategyKey)) {
            throw new IllegalArgumentException("Unknown assignment strategy: " + strategyKey +
                    ". Available strategies: " + strategies.keySet());
        }
        this.activeStrategyKey = strategyKey;
        log.info("Active technician assignment strategy changed to: {}", strategyKey);
    }

    public String getActiveStrategyKey() {
        return activeStrategyKey;
    }

    public List<Map<String, Object>> getAvailableStrategies() {
        List<Map<String, Object>> list = new ArrayList<>();

        for (String key : strategies.keySet()) {
            String name = switch (key) {
                case "skillBased" -> "Skill-Based Assignment";
                case "loadBased" -> "Load-Based (Workload Balancing)";
                case "roundRobin" -> "Round-Robin Assignment";
                default -> key;
            };
            String desc = switch (key) {
                case "skillBased" -> "Matches incident category to technician specialization, with workload tie-breaking";
                case "loadBased" -> "Assigns directly to the technician with the lowest active ticket count";
                case "roundRobin" -> "Distributes tickets evenly sequentially across all available technicians";
                default -> "Custom assignment strategy";
            };

            list.add(Map.of(
                    "key", key,
                    "name", name,
                    "description", desc,
                    "isActive", key.equalsIgnoreCase(activeStrategyKey)
            ));
        }

        return list;
    }

    public Optional<TechnicianProfile> assign(Incident incident, List<TechnicianProfile> technicians) {
        AssignmentStrategy strategy = strategies.getOrDefault(activeStrategyKey, strategies.get("skillBased"));
        if (strategy == null) {
            strategy = strategies.values().stream().findFirst().orElse(null);
        }
        if (strategy == null) {
            return Optional.empty();
        }
        return strategy.assignTechnician(incident, technicians);
    }
}
