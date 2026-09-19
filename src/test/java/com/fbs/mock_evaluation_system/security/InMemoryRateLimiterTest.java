package com.fbs.mock_evaluation_system.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.fbs.mock_evaluation_system.exception.TooManyRequestsException;

class InMemoryRateLimiterTest {

    @Test
    void allowsUpToMaxThenBlocksUntilWindowResets() {
        Instant start = Instant.parse("2026-09-19T04:00:00Z");
        MutableClock clock = new MutableClock(start);
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(clock);

        assertTrue(limiter.tryConsume("k", 2, Duration.ofMinutes(15)));
        assertTrue(limiter.tryConsume("k", 2, Duration.ofMinutes(15)));
        assertFalse(limiter.tryConsume("k", 2, Duration.ofMinutes(15)));
        assertThrows(TooManyRequestsException.class,
                () -> limiter.check("k", 2, Duration.ofMinutes(15)));

        clock.setInstant(start.plus(Duration.ofMinutes(15)));
        assertTrue(limiter.tryConsume("k", 2, Duration.ofMinutes(15)));
    }
}
