package com.fbs.mock_evaluation_system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fbs.mock_evaluation_system.dto.AuthResponseDTO;
import com.fbs.mock_evaluation_system.entity.User;
import com.fbs.mock_evaluation_system.entity.UserRole;
import com.fbs.mock_evaluation_system.exception.InvalidInputException;
import com.fbs.mock_evaluation_system.repository.UserRepository;
import com.fbs.mock_evaluation_system.security.JwtUtil;

@ExtendWith(MockitoExtension.class)
class AuthServiceOtpTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private EmailService emailService;

    private OtpService otpService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService();
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtUtil,
                emailService,
                otpService);
    }

    @Test
    void sendOtp_unknownEmail_isGenericAndDoesNotEmail() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        String message = authService.sendOtp("missing@example.com");

        assertTrue(message.contains("If an eligible account exists"));
        verify(emailService, never()).sendOtpEmail(any(), any(), any());
    }

    @Test
    void sendOtp_adminAndViewer_doNotReceiveOtp() {
        when(userRepository.findByEmail("admin@fbs.com")).thenReturn(Optional.of(user("admin@fbs.com", UserRole.ADMIN, true)));
        assertTrue(authService.sendOtp("admin@fbs.com").contains("If an eligible account exists"));
        verify(emailService, never()).sendOtpEmail(any(), any(), any());

        when(userRepository.findByEmail("viewer@fbs.com")).thenReturn(Optional.of(user("viewer@fbs.com", UserRole.VIEWER, true)));
        authService.sendOtp("viewer@fbs.com");
        verify(emailService, never()).sendOtpEmail(eq("viewer@fbs.com"), any(), any());
    }

    @Test
    void sendOtp_inactiveTrainer_doesNotEmail() {
        when(userRepository.findByEmail("trainer@example.com"))
                .thenReturn(Optional.of(user("trainer@example.com", UserRole.TRAINER, false)));

        authService.sendOtp("trainer@example.com");

        verify(emailService, never()).sendOtpEmail(any(), any(), any());
    }

    @Test
    void sendOtp_activeTrainer_sendsEmail() {
        when(userRepository.findByEmail("trainer@example.com"))
                .thenReturn(Optional.of(user("trainer@example.com", UserRole.TRAINER, true)));

        authService.sendOtp("trainer@example.com");

        verify(emailService).sendOtpEmail(eq("trainer@example.com"), eq("Pat Trainer"), any());
    }

    @Test
    void verifyOtp_deactivatedUser_doesNotIssueToken() {
        User trainer = user("trainer@example.com", UserRole.TRAINER, true);
        when(userRepository.findByEmail("trainer@example.com")).thenReturn(Optional.of(trainer));
        authService.sendOtp("trainer@example.com");
        String otp = peekOtp("trainer@example.com");
        trainer.setActive(false);

        assertThrows(InvalidInputException.class,
                () -> authService.verifyOtp("trainer@example.com", otp));
        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    void verifyOtp_activeTrainer_issuesToken() {
        User trainer = user("trainer@example.com", UserRole.TRAINER, true);
        when(userRepository.findByEmail("trainer@example.com")).thenReturn(Optional.of(trainer));
        when(jwtUtil.generateToken("trainer@example.com", "TRAINER")).thenReturn("jwt");
        authService.sendOtp("trainer@example.com");
        String otp = peekOtp("trainer@example.com");

        AuthResponseDTO response = authService.verifyOtp("trainer@example.com", otp);

        assertEquals("jwt", response.getToken());
        assertEquals(UserRole.TRAINER, response.getRole());
    }

    private String peekOtp(String email) {
        org.mockito.ArgumentCaptor<String> captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq(email), any(), captor.capture());
        return captor.getValue();
    }

    private static User user(String email, UserRole role, boolean active) {
        User user = new User();
        user.setId(9L);
        user.setEmail(email);
        user.setFullName("Pat Trainer");
        user.setRole(role);
        user.setActive(active);
        user.setPassword("hashed");
        return user;
    }
}
