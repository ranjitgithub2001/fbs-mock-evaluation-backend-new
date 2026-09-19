package com.fbs.mock_evaluation_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trainer_requests")
public class TrainerRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String qualification;

    @Column(nullable = false)
    private String expertise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrainerRequestStatus status = TrainerRequestStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column
    private LocalDateTime reviewedAt;

    @Column
    private String rejectionReason;

    // Shared attendance table requires these NOT NULL columns on insert.
    @Column(name = "request_type", nullable = false)
    private String requestType = "BATCH_ACCESS";

    @Column(name = "trainer_id", nullable = false)
    private Long trainerId = 0L;

    @PrePersist
    void fillSharedTableDefaults() {
        if (requestType == null || requestType.isBlank()) {
            requestType = "BATCH_ACCESS";
        }
        if (trainerId == null) {
            trainerId = 0L;
        }
    }

    public TrainerRequest() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    public String getExpertise() { return expertise; }
    public void setExpertise(String expertise) { this.expertise = expertise; }

    public TrainerRequestStatus getStatus() { return status; }
    public void setStatus(TrainerRequestStatus status) { this.status = status; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }

    public Long getTrainerId() { return trainerId; }
    public void setTrainerId(Long trainerId) { this.trainerId = trainerId; }
}