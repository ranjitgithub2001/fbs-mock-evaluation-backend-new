package com.fbs.mock_evaluation_system.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fbs.mock_evaluation_system.dto.ForgotPasswordRequestDTO;
import com.fbs.mock_evaluation_system.dto.ResetPasswordRequestDTO;
import com.fbs.mock_evaluation_system.dto.VerifyOtpRequestDTO;
import com.fbs.mock_evaluation_system.entity.User;
import com.fbs.mock_evaluation_system.exception.InvalidInputException;
import com.fbs.mock_evaluation_system.exception.ResourceNotFoundException;
import com.fbs.mock_evaluation_system.exception.TooManyRequestsException;
import com.fbs.mock_evaluation_system.repository.UserRepository;

@Service
public class ForgotPasswordService {

    private static final String GENERIC_SENT = "If an account exists, an OTP has been sent.";

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public ForgotPasswordService(UserRepository userRepository,
            OtpService otpService,
            EmailService emailService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    public String sendOtp(ForgotPasswordRequestDTO request) {

        String email = request.getEmail().trim().toLowerCase();

        userRepository.findByEmail(email)
                .filter(User::isActive)
                .ifPresent(user -> {
                    try {
                        String otp = otpService.generateOtp(email);
                        emailService.sendOtpEmail(email, user.getFullName(), otp);
                    } catch (TooManyRequestsException ignored) {
                        // Same generic response as unknown emails.
                    } catch (Exception ignored) {
                        // Do not leak mail failure or account existence.
                    }
                });

        return GENERIC_SENT;
    }

    public boolean verifyOtp(VerifyOtpRequestDTO request) {

        String email = request.getEmail().trim().toLowerCase();
        return otpService.verifyOtp(email, request.getOtp());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDTO request) {

        String email = request.getEmail().trim().toLowerCase();

        if (!otpService.verifyOtp(email, request.getOtp())) {
            throw new InvalidInputException("Invalid or expired OTP.");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new InvalidInputException(
                    "New password and confirm password do not match.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No account found with email: " + email));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        otpService.clearOtp(email);
    }
}