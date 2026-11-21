package com.itsm.strategy;

import com.itsm.entity.Incident;
import com.itsm.entity.TechnicianProfile;
import com.itsm.entity.User;
import com.itsm.enums.Impact;
import com.itsm.enums.Priority;
import com.itsm.enums.Urgency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StrategyPatternTests {

    private DefaultPriorityStrategy defaultStrategy;
    private WeightedPriorityStrategy weightedStrategy;
    private SkillBasedAssignmentStrategy skillStrategy;
    private LoadBasedAssignmentStrategy loadStrategy;

    @BeforeEach
    void setUp() {
        defaultStrategy = new DefaultPriorityStrategy();
        weightedStrategy = new WeightedPriorityStrategy();
        skillStrategy = new SkillBasedAssignmentStrategy();
        loadStrategy = new LoadBasedAssignmentStrategy();
    }

    // --- Priority Strategy Tests ---

    @Test
    @DisplayName("Test 1: Default Strategy - High Impact, High Urgency, Max Criticality yields P1_CRITICAL")
    void testDefaultPriority_Critical() {
        Priority priority = defaultStrategy.calculatePriority(Impact.HIGH, Urgency.HIGH, 5); // 3 * 3 * 5 = 45 >= 27
        assertEquals(Priority.P1_CRITICAL, priority);
    }

    @Test
    @DisplayName("Test 2: Default Strategy - High Impact, Low Urgency, Moderate Criticality yields P2_HIGH")
    void testDefaultPriority_High() {
        Priority priority = defaultStrategy.calculatePriority(Impact.HIGH, Urgency.LOW, 4); // 3 * 1 * 4 = 12 >= 12
        assertEquals(Priority.P2_HIGH, priority);
    }

    @Test
    @DisplayName("Test 3: Default Strategy - Low Impact, Low Urgency, Low Criticality yields P4_LOW")
    void testDefaultPriority_Low() {
        Priority priority = defaultStrategy.calculatePriority(Impact.LOW, Urgency.LOW, 1); // 1 * 1 * 1 = 1 < 6
        assertEquals(Priority.P4_LOW, priority);
    }

    @Test
    @DisplayName("Test 4: Weighted Strategy - Criticality weight calculation")
    void testWeightedPriority() {
        Priority pHigh = weightedStrategy.calculatePriority(Impact.HIGH, Urgency.HIGH, 5);
        assertEquals(Priority.P1_CRITICAL, pHigh);

        Priority pLow = weightedStrategy.calculatePriority(Impact.LOW, Urgency.LOW, 1);
        assertEquals(Priority.P4_LOW, pLow);
    }

    // --- Assignment Strategy Tests ---

    @Test
    @DisplayName("Test 5: Skill-Based Strategy - Matches technician skill to incident category")
    void testSkillBasedAssignment_MatchFound() {
        Incident incident = Incident.builder()
                .category("NETWORK")
                .build();

        TechnicianProfile tech1 = TechnicianProfile.builder()
                .id(1L)
                .skills(List.of("DATABASE", "CLOUD"))
                .currentActiveIncidents(1)
                .build();

        TechnicianProfile tech2 = TechnicianProfile.builder()
                .id(2L)
                .skills(List.of("NETWORK", "SECURITY"))
                .currentActiveIncidents(3)
                .build();

        Optional<TechnicianProfile> assigned = skillStrategy.assignTechnician(incident, List.of(tech1, tech2));
        assertTrue(assigned.isPresent());
        assertEquals(2L, assigned.get().getId());
    }

    @Test
    @DisplayName("Test 6: Skill-Based Strategy - No match returns empty optional")
    void testSkillBasedAssignment_NoMatch() {
        Incident incident = Incident.builder()
                .category("DATABASE")
                .build();

        TechnicianProfile tech1 = TechnicianProfile.builder()
                .id(1L)
                .skills(List.of("NETWORK"))
                .build();

        Optional<TechnicianProfile> assigned = skillStrategy.assignTechnician(incident, List.of(tech1));
        assertTrue(assigned.isEmpty());
    }

    @Test
    @DisplayName("Test 7: Skill-Based Strategy - Ties broken by lower workload")
    void testSkillBasedAssignment_WorkloadTieBreak() {
        Incident incident = Incident.builder()
                .category("SOFTWARE")
                .build();

        TechnicianProfile tech1 = TechnicianProfile.builder()
                .id(1L)
                .skills(List.of("SOFTWARE"))
                .currentActiveIncidents(5)
                .build();

        TechnicianProfile tech2 = TechnicianProfile.builder()
                .id(2L)
                .skills(List.of("SOFTWARE"))
                .currentActiveIncidents(2)
                .build();

        Optional<TechnicianProfile> assigned = skillStrategy.assignTechnician(incident, List.of(tech1, tech2));
        assertTrue(assigned.isPresent());
        assertEquals(2L, assigned.get().getId());
    }

    @Test
    @DisplayName("Test 8: Load-Based Strategy - Always selects technician with least active incidents")
    void testLoadBasedAssignment() {
        TechnicianProfile busy = TechnicianProfile.builder()
                .id(1L)
                .currentActiveIncidents(6)
                .build();

        TechnicianProfile free = TechnicianProfile.builder()
                .id(2L)
                .currentActiveIncidents(1)
                .build();

        Incident incident = Incident.builder().build();
        Optional<TechnicianProfile> assigned = loadStrategy.assignTechnician(incident, List.of(busy, free));

        assertTrue(assigned.isPresent());
        assertEquals(2L, assigned.get().getId());
    }

    @Test
    @DisplayName("Strategies expose a stable key, name and description for the admin screen")
    void strategiesDescribeThemselves() {
        assertEquals("skillBased", skillStrategy.getKey());
        assertEquals("loadBased", loadStrategy.getKey());
        assertEquals("defaultPriority", defaultStrategy.getKey());
        assertFalse(weightedStrategy.getDescription().isBlank());
    }

    @Test
    @DisplayName("Registry switches the active strategy and rejects unknown keys")
    void registrySwitching() {
        AssignmentStrategyManager manager = new AssignmentStrategyManager(List.of(skillStrategy, loadStrategy));
        assertEquals("skillBased", manager.getActiveStrategyKey());
        manager.setActiveStrategy("loadBased");
        assertEquals("loadBased", manager.getActiveStrategyKey());
        assertThrows(com.itsm.exception.BadRequestException.class, () -> manager.setActiveStrategy("nope"));
    }
}
