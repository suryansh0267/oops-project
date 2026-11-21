package com.itsm.chatbot;

import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory, per-user sliding window; thread-safe within each user's queue. */
@Component
public class ChatRateLimiter {
    private final Map<String, ArrayDeque<Instant>> recent = new ConcurrentHashMap<>();
    private final Clock clock;
    public ChatRateLimiter(Clock clock) { this.clock = clock; }
    public void check(String username) {
        ArrayDeque<Instant> events = recent.computeIfAbsent(username, ignored -> new ArrayDeque<>());
        synchronized (events) {
            Instant cutoff = clock.instant().minusSeconds(60);
            while (!events.isEmpty() && events.peekFirst().isBefore(cutoff)) events.removeFirst();
            if (events.size() >= 20) throw new ChatRateLimitException();
            events.addLast(clock.instant());
        }
    }
}
