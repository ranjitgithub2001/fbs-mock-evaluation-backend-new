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
import com.fbs.mock_evaluation_system.entity.User;
import com.fbs.mock_evaluation_system.repository.TrainerRequestRepository;
import com.fbs.mock_evaluation_system.repository.UserRepository;
import com.fbs.mock_evaluation_system.security.InMemoryRateLimiter;

@ExtendWith(MockitoExtension.class)
class TrainerRequestServiceApproveTest {

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
    void approveRequest_commitsBeforeSendingSetupEmail() {
        stubPendingApproval();
        when(otpService.generateOtp("ranjitkale449@gmail.com")).thenReturn("654321");

        TrainerRequestResponseDTO result = service.approveRequest(4L);

        assertEquals(TrainerRequestStatus.APPROVED, result.getStatus());
        InOrder order = inOrder(userRepository, trainerRequestRepository, transactionManager, emailService);
        order.verify(userRepository).save(any(User.class));
        order.verify(trainerRequestRepository).save(any(TrainerRequest.class));
        order.verify(transactionManager).commit(transactionStatus);
        order.verify(emailService).sendPasswordSetupEmail(
                eq("ranjitkale449@gmail.com"),
                eq("Ranjit Kale"),
                eq("654321"));
    }

    @Test
    void approveRequest_commitsWhenSetupEmailThrows() {
        stubPendingApproval();
        when(otpService.generateOtp("ranjitkale449@gmail.com")).thenReturn("654321");
        doThrow(new RuntimeException("smtp timeout"))
                .when(emailService)
                .sendPasswordSetupEmail(any(), any(), any());

        TrainerRequestResponseDTO result = service.approveRequest(4L);

        assertEquals(TrainerRequestStatus.APPROVED, result.getStatus());
        verify(transactionManager).commit(transactionStatus);
        verify(emailService).sendPasswordSetupEmail(any(), any(), any());
    }

    private void stubPendingApproval() {
        TrainerRequest pending = new TrainerRequest();
        pending.setId(4L);
        pending.setFullName("Ranjit Kale");
        pending.setEmail("ranjitkale449@gmail.com");
        pending.setStatus(TrainerRequestStatus.PENDING);
        when(trainerRequestRepository.findById(4L)).thenReturn(Optional.of(pending));
        when(userRepository.existsByEmail("ranjitkale449@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed-random");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(trainerRequestRepository.save(any(TrainerRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
}
