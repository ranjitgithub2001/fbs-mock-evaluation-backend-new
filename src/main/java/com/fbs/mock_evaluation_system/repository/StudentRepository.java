package com.fbs.mock_evaluation_system.repository;

import com.fbs.mock_evaluation_system.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    // ── Active-only queries (used by StudentService) ──────────────────────────
    Page<Student> findByActiveTrue(Pageable pageable);
    Page<Student> findByBatchIdAndActiveTrue(Long batchId, Pageable pageable);
    Optional<Student> findByFrnAndActiveTrue(String frn);

    // ── All students in batch (used by AnalyticsService) ─────────────────────
    List<Student> findByBatchId(Long batchId);
    long countByBatchId(Long batchId);

    // ── Keep original for backward compat ─────────────────────────────────────
    Optional<Student> findByFrn(String frn);
}