package com.fbs.mock_evaluation_system.controller;

import com.fbs.mock_evaluation_system.dto.AuthRequestDTO;
import com.fbs.mock_evaluation_system.dto.AuthResponseDTO;
import com.fbs.mock_evaluation_system.dto.ForgotPasswordRequestDTO;
import com.fbs.mock_evaluation_system.dto.ResetPasswordRequestDTO;
import com.fbs.mock_evaluation_system.dto.VerifyOtpRequestDTO;
import com.fbs.mock_evaluation_system.service.AuthService;
import com.fbs.mock_evaluation_system.service.ForgotPasswordService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final ForgotPasswordService forgotPasswordService;

    public AuthController(AuthService authService,
            ForgotPasswordService forgotPasswordService) {
        this.authService = authService;
        this.forgotPasswordService = forgotPasswordService;
    }

    // POST /api/auth/login — password login for all roles
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody AuthRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // POST /api/auth/otp/send — TRAINER, PLACEMENT (OTP)
    @PostMapping("/otp/send")
    public ResponseEntity<Map<String, String>> sendOtp(
            @RequestBody Map<String, String> body) {
        String message = authService.sendOtp(body.get("email"));
        return ResponseEntity.ok(Map.of("message", message));
    }

    // POST /api/auth/otp/verify — verify OTP and get token
    @PostMapping("/otp/verify")
    public ResponseEntity<AuthResponseDTO> verifyOtp(
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
            authService.verifyOtp(body.get("email"), body.get("otp")));
    }

    // POST /api/auth/forgot-password
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO request) {
        return ResponseEntity.ok(Map.of("message", forgotPasswordService.sendOtp(request)));
    }

    // POST /api/auth/verify-otp
    @PostMapping("/verify-otp")
    public ResponseEntity<Boolean> verifyOtp(
            @Valid @RequestBody VerifyOtpRequestDTO request) {
        boolean valid = forgotPasswordService.verifyOtp(request);
        return ResponseEntity.ok(valid);
    }

    // POST /api/auth/reset-password
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO request) {
        forgotPasswordService.resetPassword(request);
        return ResponseEntity.ok().build();
    }
}