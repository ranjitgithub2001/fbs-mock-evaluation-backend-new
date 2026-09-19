package com.fbs.mock_evaluation_system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.fbs.mock_evaluation_system.exception.TooManyRequestsException;
import com.fbs.mock_evaluation_system.security.MutableClock;

class OtpServiceTest {

    @Test
    void expiresAfterTtl() {
        Instant start = Instant.parse("2026-09-19T04:00:00Z");
        MutableClock clock = new MutableClock(start);
        OtpService otpService = new OtpService(clock);

        String otp = otpService.generateOtp("trainer@example.com");
        assertTrue(otpService.verifyOtp("trainer@example.com", otp));

        clock.setInstant(start.plus(OtpService.OTP_TTL));
        assertFalse(otpService.verifyOtp("trainer@example.com", otp));
    }

    @Test
    void burnsOtpAfterMaxFailedAttempts() {
        OtpService otpService = new OtpService();
        String otp = otpService.generateOtp("trainer@example.com");

        for (int i = 0; i < OtpService.MAX_VERIFY_ATTEMPTS; i++) {
            assertFalse(otpService.verifyOtp("trainer@example.com", "000000"));
        }
        assertFalse(otpService.verifyOtp("trainer@example.com", otp));
    }

    @Test
    void enforcesResendCooldown() {
        OtpService otpService = new OtpService();
        otpService.generateOtp("trainer@example.com");

        TooManyRequestsException ex = assertThrows(TooManyRequestsException.class,
                () -> otpService.generateOtp("trainer@example.com"));
        assertEquals(com.fbs.mock_evaluation_system.security.InMemoryRateLimiter.GENERIC_LIMIT_MESSAGE,
                ex.getMessage());
    }

    @Test
    void allowsResendAfterCooldown() {
        Instant start = Instant.parse("2026-09-19T04:00:00Z");
        MutableClock clock = new MutableClock(start);
        OtpService otpService = new OtpService(clock);

        otpService.generateOtp("trainer@example.com");
        clock.setInstant(start.plus(OtpService.RESEND_COOLDOWN));
        String next = otpService.generateOtp("trainer@example.com");
        assertTrue(otpService.verifyOtp("trainer@example.com", next));
    }

    @Test
    void otpIsSixDigits() {
        String otp = new OtpService().generateOtp("trainer@example.com");
        assertTrue(otp.matches("\\d{6}"));
    }

    @Test
    void cooldownUsesDurationConstant() {
        assertEquals(Duration.ofSeconds(60), OtpService.RESEND_COOLDOWN);
        assertEquals(Duration.ofMinutes(10), OtpService.OTP_TTL);
        assertEquals(5, OtpService.MAX_VERIFY_ATTEMPTS);
    }
}
