package com.fbs.mock_evaluation_system.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fbs.mock_evaluation_system.dto.ForgotPasswordRequestDTO;
import com.fbs.mock_evaluation_system.entity.User;
import com.fbs.mock_evaluation_system.entity.UserRole;
import com.fbs.mock_evaluation_system.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private OtpService otpService;
    private ForgotPasswordService service;

    @BeforeEach
    void setUp() {
        otpService = new OtpService();
        service = new ForgotPasswordService(
                userRepository,
                otpService,
                emailService,
                passwordEncoder);
    }

    @Test
    void sendOtp_neverReturnsTheCode_andIsGenericForUnknownEmail() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO();
        request.setEmail("missing@example.com");

        String response = service.sendOtp(request);

        assertTrue(response.contains("If an account exists"));
        assertFalse(response.matches(".*\\d{6}.*"));
        verify(emailService, never()).sendOtpEmail(any(), any(), any());
    }

    @Test
    void sendOtp_emailsCodeButDoesNotReturnIt() {
        User user = new User();
        user.setEmail("admin@fbs.com");
        user.setFullName("Admin User");
        user.setRole(UserRole.ADMIN);
        user.setActive(true);
        when(userRepository.findByEmail("admin@fbs.com")).thenReturn(Optional.of(user));

        ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO();
        request.setEmail("admin@fbs.com");

        String response = service.sendOtp(request);

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq("admin@fbs.com"), eq("Admin User"), otpCaptor.capture());
        assertTrue(otpCaptor.getValue().matches("\\d{6}"));
        assertFalse(response.contains(otpCaptor.getValue()));
        assertTrue(response.contains("If an account exists"));
    }
}
