package com.fbs.mock_evaluation_system.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;

import com.fbs.mock_evaluation_system.dto.TrainerRequestDTO;
import com.fbs.mock_evaluation_system.entity.TrainerRequest;
import com.fbs.mock_evaluation_system.exception.TooManyRequestsException;
import com.fbs.mock_evaluation_system.repository.TrainerRequestRepository;
import com.fbs.mock_evaluation_system.repository.UserRepository;
import com.fbs.mock_evaluation_system.security.InMemoryRateLimiter;

@ExtendWith(MockitoExtension.class)
class TrainerRequestServiceRateLimitTest {

    @Mock
    private TrainerRequestRepository trainerRequestRepository;
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

    private TrainerRequestService service;

    @BeforeEach
    void setUp() {
        service = new TrainerRequestService(
                trainerRequestRepository,
                userRepository,
                passwordEncoder,
                emailService,
                new InMemoryRateLimiter(),
                otpService,
                transactionManager);
    }

    @Test
    void submitRequest_blocksFourthAttemptForSameEmail() {
        when(trainerRequestRepository.existsByEmailAndStatusIn(any(), any())).thenReturn(false);
        when(userRepository.existsByEmail("new.trainer@example.com")).thenReturn(false);
        when(trainerRequestRepository.save(any(TrainerRequest.class))).thenAnswer(invocation -> {
            TrainerRequest saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        TrainerRequestDTO request = request();
        service.submitRequest(request);
        service.submitRequest(request);
        service.submitRequest(request);

        assertThrows(TooManyRequestsException.class, () -> service.submitRequest(request));
    }

    private static TrainerRequestDTO request() {
        TrainerRequestDTO dto = new TrainerRequestDTO();
        dto.setFullName("New Trainer");
        dto.setEmail("new.trainer@example.com");
        dto.setPhone("9876543210");
        dto.setQualification("B.E.");
        dto.setExpertise("Java");
        return dto;
    }
}
