package com.fbs.mock_evaluation_system.service;

import com.fbs.mock_evaluation_system.dto.AuthRequestDTO;
import com.fbs.mock_evaluation_system.dto.AuthResponseDTO;
import com.fbs.mock_evaluation_system.entity.User;
import com.fbs.mock_evaluation_system.entity.UserRole;
import com.fbs.mock_evaluation_system.exception.InvalidInputException;
import com.fbs.mock_evaluation_system.repository.UserRepository;
import com.fbs.mock_evaluation_system.security.JwtUtil;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

@Service
public class AuthService {

    static final Set<UserRole> OTP_LOGIN_ROLES = EnumSet.of(UserRole.TRAINER, UserRole.PLACEMENT);
    private static final String GENERIC_OTP_SENT = "If an eligible account exists, an OTP has been sent.";
    private static final String GENERIC_OTP_INVALID = "Invalid or expired OTP. Please request a new one.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final OtpService otpService;

    public AuthService(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            EmailService emailService,
            OtpService otpService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.otpService = otpService;
    }

    // ── Password login (ADMIN, TRAINER, PLACEMENT, and other roles) ─────────
    @Transactional(readOnly = true)
    public AuthResponseDTO login(AuthRequestDTO request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidInputException("Invalid email or password"));

        if (!user.isActive())
            throw new InvalidInputException("Your account has been deactivated. Please contact admin.");

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new InvalidInputException(
                    "No password is set for this account. Use Forgot Password or contact admin.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new InvalidInputException("Invalid email or password");

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return new AuthResponseDTO(token, user.getId(), user.getFullName(),
                user.getEmail(), user.getRole());
    }

    // ── OTP Send (TRAINER, PLACEMENT only). Always generic to callers. ─────
    public String sendOtp(String email) {
        if (email == null || email.isBlank())
            throw new InvalidInputException("Email is required");

        String normalizedEmail = email.trim().toLowerCase();

        userRepository.findByEmail(normalizedEmail)
                .filter(this::canReceiveLoginOtp)
                .ifPresent(user -> {
                    try {
                        String otp = otpService.generateOtp(normalizedEmail);
                        emailService.sendOtpEmail(normalizedEmail, user.getFullName(), otp);
                    } catch (Exception ignored) {
                        // Do not leak whether the account exists, cooldown, or mail failed.
                    }
                });

        return GENERIC_OTP_SENT;
    }

    // ── OTP Verify ────────────────────────────────────────────────────────────
    public AuthResponseDTO verifyOtp(String email, String otp) {
        if (email == null || otp == null || email.isBlank() || otp.isBlank())
            throw new InvalidInputException("Email and OTP are required");

        String normalizedEmail = email.trim().toLowerCase();

        if (!otpService.verifyOtp(normalizedEmail, otp.trim())) {
            throw new InvalidInputException(GENERIC_OTP_INVALID);
        }

        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null || !canReceiveLoginOtp(user)) {
            otpService.clearOtp(normalizedEmail);
            throw new InvalidInputException(GENERIC_OTP_INVALID);
        }

        otpService.clearOtp(normalizedEmail);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return new AuthResponseDTO(token, user.getId(), user.getFullName(),
                user.getEmail(), user.getRole());
    }

    boolean canReceiveLoginOtp(User user) {
        return user.isActive() && OTP_LOGIN_ROLES.contains(user.getRole());
    }
}
