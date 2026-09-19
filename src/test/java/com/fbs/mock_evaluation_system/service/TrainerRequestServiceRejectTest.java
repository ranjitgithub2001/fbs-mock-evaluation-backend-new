package com.fbs.mock_evaluation_system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import com.fbs.mock_evaluation_system.dto.TrainerRequestResponseDTO;
import com.fbs.mock_evaluation_system.entity.TrainerRequest;
import com.fbs.mock_evaluation_system.entity.TrainerRequestStatus;
import com.fbs.mock_evaluation_system.repository.TrainerRequestRepository;
import com.fbs.mock_evaluation_system.repository.UserRepository;
import com.fbs.mock_evaluation_system.security.InMemoryRateLimiter;

@ExtendWith(MockitoExtension.class)
class TrainerRequestServiceRejectTest {

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
    @Mock
    private TransactionStatus transactionStatus;

    private TrainerRequestService service;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
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
    void rejectRequest_commitsBeforeSendingEmail() {
        stubPendingRejection();

        TrainerRequestResponseDTO result = service.rejectRequest(4L, "Incomplete profile");

        assertEquals(TrainerRequestStatus.REJECTED, result.getStatus());
        InOrder order = inOrder(trainerRequestRepository, transactionManager, emailService);
        order.verify(trainerRequestRepository).save(any(TrainerRequest.class));
        order.verify(transactionManager).commit(transactionStatus);
        order.verify(emailService).sendRejectionEmail(
                eq("ranjitkale449@gmail.com"),
                eq("Ranjit Kale"),
                eq("Incomplete profile"));
    }

    @Test
    void rejectRequest_commitsWhenSmtpFails() {
        stubPendingRejection();
        doThrow(new RuntimeException("smtp timeout"))
                .when(emailService)
                .sendRejectionEmail(any(), any(), any());

        TrainerRequestResponseDTO result = service.rejectRequest(4L, "Incomplete profile");

        assertEquals(TrainerRequestStatus.REJECTED, result.getStatus());
        assertEquals("Incomplete profile", result.getRejectionReason());
        verify(transactionManager).commit(transactionStatus);
        verify(emailService).sendRejectionEmail(
                "ranjitkale449@gmail.com",
                "Ranjit Kale",
                "Incomplete profile");
    }

    private void stubPendingRejection() {
        TrainerRequest pending = new TrainerRequest();
        pending.setId(4L);
        pending.setFullName("Ranjit Kale");
        pending.setEmail("ranjitkale449@gmail.com");
        pending.setStatus(TrainerRequestStatus.PENDING);
        when(trainerRequestRepository.findById(4L)).thenReturn(Optional.of(pending));
        when(trainerRequestRepository.save(any(TrainerRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
}
