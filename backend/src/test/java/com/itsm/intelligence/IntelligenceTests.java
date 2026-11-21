package com.itsm.intelligence;

import com.itsm.enums.Priority;
import com.itsm.strategy.PriorityCalculationStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IntelligenceTests {

    @Test
    @DisplayName("Classifier: VPN / Wi-Fi text is categorised as NETWORK")
    void classifiesNetwork() {
        CategoryClassifier.Suggestion s = CategoryClassifier.classify(
                "VPN keeps disconnecting and wifi is slow", CategoryClassifier.DEFAULT_KEYWORDS);
        assertEquals("NETWORK", s.category());
        assertTrue(s.confidence() > 0);
        assertTrue(s.matchedKeywords().contains("vpn"));
    }

    @Test
    @DisplayName("Classifier: admin-defined category and keywords are used without code changes")
    void customCategory() {
        Map<String, List<String>> map = Map.of("PRINTING", List.of("printer", "toner"), "NETWORK", List.of("wifi"));
        assertEquals("PRINTING", CategoryClassifier.classify("Printer out of toner", map).category());
    }

    @Test
    @DisplayName("Classifier: no keyword match falls back to OTHER with zero confidence")
    void fallback() {
        CategoryClassifier.Suggestion s = CategoryClassifier.classify("hello there", CategoryClassifier.DEFAULT_KEYWORDS);
        assertEquals(CategoryClassifier.FALLBACK, s.category());
        assertEquals(0.0, s.confidence());
    }

    @Test
    @DisplayName("Similarity: stemming matches 'payments failing' with 'payment failed'")
    void stemming() {
        assertTrue(TextSimilarity.jaccard("payments failing", "payment failed") >= 0.99);
        assertEquals(0.0, TextSimilarity.jaccard("printer jam", "database deadlock"));
        assertEquals(0.0, TextSimilarity.jaccard(null, "anything"));
    }

    @Test
    @DisplayName("Scorer: a matching, idle technician beats an unskilled busy one")
    void scorer() {
        TechnicianScorer.Score good = TechnicianScorer.score(true, 1, 10, 20, 3, true);
        TechnicianScorer.Score poor = TechnicianScorer.score(false, 9, 10, 2, 0, true);
        assertTrue(good.total() > poor.total());
        assertTrue(good.total() <= 100);
        assertEquals(0, TechnicianScorer.score(false, 5, 0, 0, 0, false).availability());
    }

    @Test
    @DisplayName("Priority: many affected users raises priority, never above P1")
    void affectedUsersRaisePriority() {
        assertEquals(Priority.P3_MEDIUM, PriorityCalculationStrategy.raiseForAffectedUsers(Priority.P3_MEDIUM, 10));
        assertEquals(Priority.P2_HIGH, PriorityCalculationStrategy.raiseForAffectedUsers(Priority.P3_MEDIUM, 60));
        assertEquals(Priority.P1_CRITICAL, PriorityCalculationStrategy.raiseForAffectedUsers(Priority.P3_MEDIUM, 800));
        assertEquals(Priority.P1_CRITICAL, PriorityCalculationStrategy.raiseForAffectedUsers(Priority.P1_CRITICAL, 800));
        assertEquals(Priority.P4_LOW, PriorityCalculationStrategy.raiseForAffectedUsers(Priority.P4_LOW, null));
    }
}
