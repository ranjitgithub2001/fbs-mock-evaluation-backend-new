package com.fbs.mock_evaluation_system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import com.fbs.mock_evaluation_system.dto.UserRequestDTO;
import com.fbs.mock_evaluation_system.dto.UserResponseDTO;
import com.fbs.mock_evaluation_system.entity.User;
import com.fbs.mock_evaluation_system.entity.UserRole;
import com.fbs.mock_evaluation_system.exception.DuplicateResourceException;
import com.fbs.mock_evaluation_system.mapper.UserMapper;
import com.fbs.mock_evaluation_system.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceCreateUserTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @Mock
    private OtpService otpService;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private TransactionStatus transactionStatus;

    private UserService userService;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        userService = new UserService(
                userRepository,
                new UserMapper(),
                passwordEncoder,
                emailService,
                otpService,
                transactionManager);
    }

    @Test
    void createUser_commitsBeforeSendingSetupEmail() {
        UserRequestDTO request = request("TEST Cursor QA Trainer", "test.cursor.qa@example.com");
        when(userRepository.existsByEmail("test.cursor.qa@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(99L);
            return user;
        });
        when(otpService.generateOtp("test.cursor.qa@example.com")).thenReturn("123456");

        UserResponseDTO result = userService.createUser(request);

        assertEquals(99L, result.getId());
        assertEquals("test.cursor.qa@example.com", result.getEmail());
        assertEquals(UserRole.TRAINER, result.getRole());
        assertTrue(result.isActive());

        InOrder order = Mockito.inOrder(userRepository, transactionManager, emailService);
        order.verify(userRepository).save(any(User.class));
        order.verify(transactionManager).commit(transactionStatus);
        order.verify(emailService).sendPasswordSetupEmail(
                eq("test.cursor.qa@example.com"),
                eq("TEST Cursor QA Trainer"),
                eq("123456"));
    }

    @Test
    void createUser_succeedsWhenSetupEmailThrows() {
        UserRequestDTO request = request("TEST Cursor QA Trainer", "test.cursor.qa@example.com");
        when(userRepository.existsByEmail("test.cursor.qa@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(99L);
            return user;
        });
        when(otpService.generateOtp("test.cursor.qa@example.com")).thenReturn("123456");
        doThrow(new RuntimeException("smtp timeout"))
                .when(emailService)
                .sendPasswordSetupEmail(any(), any(), any());

        UserResponseDTO result = userService.createUser(request);

        assertEquals(99L, result.getId());
        verify(transactionManager).commit(transactionStatus);
        verify(emailService).sendPasswordSetupEmail(any(), any(), any());
    }

    @Test
    void createUser_doesNotEmailWhenDuplicate() {
        UserRequestDTO request = request("TEST Cursor QA Trainer", "test.cursor.qa@example.com");
        when(userRepository.existsByEmail("test.cursor.qa@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.createUser(request));

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendPasswordSetupEmail(any(), any(), any());
    }

    private static UserRequestDTO request(String name, String email) {
        UserRequestDTO request = new UserRequestDTO();
        request.setFullName(name);
        request.setEmail(email);
        request.setRole(UserRole.TRAINER);
        return request;
    }
}
