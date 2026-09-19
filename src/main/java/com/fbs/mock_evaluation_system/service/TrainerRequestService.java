package com.fbs.mock_evaluation_system.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.fbs.mock_evaluation_system.dto.TrainerRequestDTO;
import com.fbs.mock_evaluation_system.dto.TrainerRequestResponseDTO;
import com.fbs.mock_evaluation_system.entity.TrainerRequest;
import com.fbs.mock_evaluation_system.entity.TrainerRequestStatus;
import com.fbs.mock_evaluation_system.entity.User;
import com.fbs.mock_evaluation_system.entity.UserRole;
import com.fbs.mock_evaluation_system.exception.DuplicateResourceException;
import com.fbs.mock_evaluation_system.exception.InvalidInputException;
import com.fbs.mock_evaluation_system.exception.ResourceNotFoundException;
import com.fbs.mock_evaluation_system.repository.TrainerRequestRepository;
import com.fbs.mock_evaluation_system.repository.UserRepository;
import com.fbs.mock_evaluation_system.security.InMemoryRateLimiter;
import com.fbs.mock_evaluation_system.security.SecurePasswords;

@Service
public class TrainerRequestService {

    private static final Logger log = LoggerFactory.getLogger(TrainerRequestService.class);

    static final int REGISTER_EMAIL_MAX = 3;
    static final Duration REGISTER_EMAIL_WINDOW = Duration.ofHours(1);

    private final TrainerRequestRepository trainerRequestRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final InMemoryRateLimiter rateLimiter;
    private final OtpService otpService;
    private final TransactionTemplate transactionTemplate;

    public TrainerRequestService(
            TrainerRequestRepository trainerRequestRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            InMemoryRateLimiter rateLimiter,
            OtpService otpService,
            PlatformTransactionManager transactionManager) {
        this.trainerRequestRepository = trainerRequestRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.rateLimiter = rateLimiter;
        this.otpService = otpService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional
    public TrainerRequestResponseDTO submitRequest(TrainerRequestDTO request) {

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        rateLimiter.check("register-email:" + normalizedEmail, REGISTER_EMAIL_MAX, REGISTER_EMAIL_WINDOW);

        if (trainerRequestRepository.existsByEmailAndStatusIn(
                normalizedEmail,
                java.util.List.of(TrainerRequestStatus.PENDING, TrainerRequestStatus.APPROVED))) {
            throw new DuplicateResourceException(
                    "A request with this email already exists");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException(
                    "An account with this email already exists");
        }

        TrainerRequest trainerRequest = new TrainerRequest();
        trainerRequest.setFullName(request.getFullName().trim());
        trainerRequest.setEmail(normalizedEmail);
        trainerRequest.setPhone(request.getPhone().trim());
        trainerRequest.setQualification(request.getQualification().trim());
        trainerRequest.setExpertise(request.getExpertise().trim());
        trainerRequest.setStatus(TrainerRequestStatus.PENDING);
        trainerRequest.setRequestedAt(LocalDateTime.now());
        trainerRequest.setRequestType("BATCH_ACCESS");
        trainerRequest.setTrainerId(0L);

        TrainerRequest saved = trainerRequestRepository.save(trainerRequest);

        return toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public Page<TrainerRequestResponseDTO> getRequests(
            TrainerRequestStatus status, Pageable pageable) {

        Page<TrainerRequest> page;

        if (status != null) {
            page = trainerRequestRepository.findByStatus(status, pageable);
        } else {
            page = trainerRequestRepository.findAll(pageable);
        }

        return page.map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public TrainerRequestResponseDTO getRequestById(Long id) {

        TrainerRequest request = trainerRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Trainer request not found with id: " + id));

        return toResponseDTO(request);
    }

    public TrainerRequestResponseDTO approveRequest(Long id) {

        TrainerRequest saved = transactionTemplate.execute(status -> {
            TrainerRequest request = trainerRequestRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Trainer request not found with id: " + id));

            if (request.getStatus() != TrainerRequestStatus.PENDING) {
                throw new InvalidInputException(
                        "Only PENDING requests can be approved");
            }

            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException(
                        "An account with this email already exists");
            }

            User user = new User();
            user.setFullName(request.getFullName());
            user.setEmail(request.getEmail());
            user.setRole(UserRole.TRAINER);
            user.setActive(true);
            user.setPassword(passwordEncoder.encode(SecurePasswords.randomUnusableSecret()));

            userRepository.save(user);

            request.setStatus(TrainerRequestStatus.APPROVED);
            request.setReviewedAt(LocalDateTime.now());
            return trainerRequestRepository.save(request);
        });

        try {
            String setupOtp = otpService.generateOtp(saved.getEmail());
            emailService.sendPasswordSetupEmail(saved.getEmail(), saved.getFullName(), setupOtp);
        } catch (Exception e) {
            log.error("Failed to send password setup email after approving {}", saved.getEmail(), e);
        }

        return toResponseDTO(saved);
    }

    public TrainerRequestResponseDTO rejectRequest(Long id, String reason) {

        TrainerRequest saved = transactionTemplate.execute(status -> {
            TrainerRequest request = trainerRequestRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Trainer request not found with id: " + id));

            if (request.getStatus() != TrainerRequestStatus.PENDING) {
                throw new InvalidInputException(
                        "Only PENDING requests can be rejected");
            }

            request.setStatus(TrainerRequestStatus.REJECTED);
            request.setReviewedAt(LocalDateTime.now());
            request.setRejectionReason(reason);
            return trainerRequestRepository.save(request);
        });

        try {
            emailService.sendRejectionEmail(
                    saved.getEmail(),
                    saved.getFullName(),
                    reason);
        } catch (Exception e) {
            log.error("Failed to send rejection email after rejecting {}", saved.getEmail(), e);
        }

        return toResponseDTO(saved);
    }

    private TrainerRequestResponseDTO toResponseDTO(TrainerRequest request) {

        TrainerRequestResponseDTO dto = new TrainerRequestResponseDTO();
        dto.setId(request.getId());
        dto.setFullName(request.getFullName());
        dto.setEmail(request.getEmail());
        dto.setPhone(request.getPhone());
        dto.setQualification(request.getQualification());
        dto.setExpertise(request.getExpertise());
        dto.setStatus(request.getStatus());
        dto.setRequestedAt(request.getRequestedAt());
        dto.setReviewedAt(request.getReviewedAt());
        dto.setRejectionReason(request.getRejectionReason());
        return dto;
    }
}