package com.fbs.mock_evaluation_system.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.fbs.mock_evaluation_system.exception.TooManyRequestsException;
import com.fbs.mock_evaluation_system.security.InMemoryRateLimiter;

@Service
public class OtpService {

    static final Duration OTP_TTL = Duration.ofMinutes(10);
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    static final int MAX_VERIFY_ATTEMPTS = 5;

    private static class OtpEntry {
        final String otp;
        final Instant expiry;
        final Instant lastSent;
        int failedAttempts;

        OtpEntry(String otp, Instant expiry, Instant lastSent) {
            this.otp = otp;
            this.expiry = expiry;
            this.lastSent = lastSent;
        }
    }

    private final ConcurrentHashMap<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    public OtpService() {
        this(Clock.systemUTC());
    }

    OtpService(Clock clock) {
        this.clock = clock;
    }

    public String generateOtp(String email) {
        String key = email.toLowerCase();
        Instant now = clock.instant();
        OtpEntry existing = otpStore.get(key);
        if (existing != null && existing.lastSent.plus(RESEND_COOLDOWN).isAfter(now)) {
            throw new TooManyRequestsException(InMemoryRateLimiter.GENERIC_LIMIT_MESSAGE);
        }

        String otp = String.format("%06d", random.nextInt(1_000_000));
        otpStore.put(key, new OtpEntry(otp, now.plus(OTP_TTL), now));
        return otp;
    }

    public boolean verifyOtp(String email, String otp) {
        String key = email.toLowerCase();
        OtpEntry entry = otpStore.get(key);
        if (entry == null) {
            return false;
        }
        Instant now = clock.instant();
        if (!now.isBefore(entry.expiry)) {
            otpStore.remove(key);
            return false;
        }
        if (entry.failedAttempts >= MAX_VERIFY_ATTEMPTS) {
            otpStore.remove(key);
            return false;
        }
        if (!entry.otp.equals(otp)) {
            entry.failedAttempts++;
            if (entry.failedAttempts >= MAX_VERIFY_ATTEMPTS) {
                otpStore.remove(key);
            }
            return false;
        }
        return true;
    }

    public void clearOtp(String email) {
        otpStore.remove(email.toLowerCase());
    }
}
