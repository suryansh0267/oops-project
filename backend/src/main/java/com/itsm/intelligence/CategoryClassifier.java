package com.itsm.intelligence;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Keyword-based incident categorisation (intelligent feature F).
 * Pure logic: the caller supplies the category -> keyword map, which comes from
 * the admin-managed category table, so new categories need no code change.
 */
public final class CategoryClassifier {

    public record Suggestion(String category, double confidence, List<String> matchedKeywords) {
    }

    /** Built-in keywords used when a category has none configured. */
    public static final Map<String, List<String>> DEFAULT_KEYWORDS = Map.ofEntries(
            Map.entry("HARDWARE", List.of("laptop", "desktop", "monitor", "keyboard", "mouse", "printer", "battery", "screen", "hardware", "projector", "disk")),
            Map.entry("SOFTWARE", List.of("install", "license", "update", "software", "crash", "excel", "office", "outlook", "upgrade", "driver")),
            Map.entry("NETWORK", List.of("network", "wifi", "wi-fi", "internet", "vpn", "lan", "router", "switch", "dns", "latency", "packet", "connectivity", "ethernet")),
            Map.entry("DATABASE", List.of("database", "db", "sql", "query", "postgres", "mysql", "oracle", "deadlock", "replication", "table", "schema")),
            Map.entry("CLOUD", List.of("cloud", "aws", "azure", "gcp", "kubernetes", "container", "docker", "cluster", "instance", "s3", "lambda")),
            Map.entry("SECURITY", List.of("virus", "malware", "phishing", "breach", "hack", "suspicious", "ransomware", "firewall", "attack", "spam")),
            Map.entry("AUTHENTICATION", List.of("login", "password", "sso", "otp", "2fa", "mfa", "locked", "authentication", "sign", "token", "credential", "ldap")),
            Map.entry("APPLICATION", List.of("app", "application", "portal", "website", "page", "payment", "erp", "button", "form", "error", "500", "timeout", "transaction")),
            Map.entry("STORAGE", List.of("storage", "drive", "quota", "backup", "file", "share", "nas", "space", "upload", "folder")));

    public static final String FALLBACK = "OTHER";

    private CategoryClassifier() {
    }

    public static Suggestion classify(String text, Map<String, ? extends Collection<String>> keywordsByCategory) {
        Set<String> tokens = TextSimilarity.tokenize(text);
        String lower = text == null ? "" : text.toLowerCase(Locale.ROOT);

        String best = FALLBACK;
        List<String> bestMatches = List.of();
        int total = 0;
        Map<String, Integer> scores = new LinkedHashMap<>();

        for (Map.Entry<String, ? extends Collection<String>> entry : keywordsByCategory.entrySet()) {
            List<String> matches = entry.getValue().stream()
                    .map(k -> k.toLowerCase(Locale.ROOT).trim())
                    .filter(k -> !k.isEmpty())
                    .filter(k -> k.contains(" ") || k.contains("-") ? lower.contains(k)
                            : tokens.contains(TextSimilarity.stem(k)) || tokens.contains(k))
                    .distinct()
                    .toList();
            scores.put(entry.getKey(), matches.size());
            total += matches.size();
            if (matches.size() > bestMatches.size()) {
                best = entry.getKey();
                bestMatches = matches;
            }
        }

        if (bestMatches.isEmpty()) {
            return new Suggestion(FALLBACK, 0.0, List.of());
        }
        // purity: how much of the evidence points at the winner; coverage: more hits, more certainty.
        int n = bestMatches.size();
        double purity = (double) n / Math.max(total, 1);
        double coverage = (double) n / (n + 1);
        double confidence = Math.min(0.95, purity * coverage);
        return new Suggestion(best, Math.round(confidence * 100) / 100.0, bestMatches);
    }
}
