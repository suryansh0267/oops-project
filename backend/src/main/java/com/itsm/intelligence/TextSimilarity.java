package com.itsm.intelligence;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Token-based Jaccard similarity with stop-word removal and light stemming,
 * so "payment failing" and "payments failed" share tokens.
 */
public final class TextSimilarity {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "in", "on", "at", "to", "for", "with", "by", "about", "from",
            "is", "are", "was", "were", "be", "been", "and", "or", "not", "of", "it", "its",
            "my", "our", "we", "i", "this", "that", "can", "cannot", "can't", "unable", "please",
            "when", "while", "after", "all", "any", "some", "no", "getting", "get", "has", "have");

    private TextSimilarity() {
    }

    public static Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(w -> w.length() > 1 && !STOP_WORDS.contains(w))
                .map(TextSimilarity::stem)
                .collect(Collectors.toSet());
    }

    static String stem(String word) {
        if (word.length() > 5 && word.endsWith("ing")) {
            return word.substring(0, word.length() - 3);
        }
        if (word.length() > 4 && word.endsWith("ed")) {
            return word.substring(0, word.length() - 2);
        }
        if (word.length() > 3 && word.endsWith("s") && !word.endsWith("ss")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }

    public static double jaccard(String a, String b) {
        return jaccard(tokenize(a), tokenize(b));
    }

    public static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }

    /**
     * Weighted score used for duplicate detection: title matters more than description,
     * with bonuses when the incidents hit the same service or category.
     */
    public static double incidentScore(String titleA, String descA, String titleB, String descB,
                                       boolean sameService, boolean sameCategory) {
        double score = jaccard(titleA, titleB) * 0.6 + jaccard(descA, descB) * 0.4;
        if (sameService) {
            score += 0.15;
        }
        if (sameCategory) {
            score += 0.10;
        }
        return Math.min(1.0, score);
    }
}
