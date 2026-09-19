package com.fbs.mock_evaluation_system.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.fbs.mock_evaluation_system.exception.TooManyRequestsException;

@Component
public class InMemoryRateLimiter {

    public static final String GENERIC_LIMIT_MESSAGE =
            "Too many requests. Please try again later.";

    private record Window(int count, Instant start) {
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryRateLimiter() {
        this(Clock.systemUTC());
    }

    InMemoryRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public void check(String key, int maxAttempts, Duration window) {
        if (!tryConsume(key, maxAttempts, window)) {
            throw new TooManyRequestsException(GENERIC_LIMIT_MESSAGE);
        }
    }

    public boolean tryConsume(String key, int maxAttempts, Duration window) {
        Instant now = clock.instant();
        while (true) {
            Window current = windows.get(key);
            if (current == null || !current.start.plus(window).isAfter(now)) {
                Window fresh = new Window(1, now);
                if (current == null) {
                    if (windows.putIfAbsent(key, fresh) == null) {
                        return true;
                    }
                } else if (windows.replace(key, current, fresh)) {
                    return true;
                }
                continue;
            }
            if (current.count >= maxAttempts) {
                return false;
            }
            Window next = new Window(current.count + 1, current.start);
            if (windows.replace(key, current, next)) {
                return true;
            }
        }
    }
}
