package com.fbs.mock_evaluation_system.controller;

import com.fbs.mock_evaluation_system.dto.BatchOverviewDTO;
import com.fbs.mock_evaluation_system.dto.BatchProgressReportDTO;
import com.fbs.mock_evaluation_system.dto.ModulePerformanceDTO;
import com.fbs.mock_evaluation_system.dto.StudentPerformanceDTO;
import com.fbs.mock_evaluation_system.dto.TrainerPerformanceDTO;
import com.fbs.mock_evaluation_system.service.AnalyticsService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    // GET /analytics/batch/{batchId}/overview
    @GetMapping("/batch/{batchId}/overview")
    public ResponseEntity<BatchOverviewDTO> getBatchOverview(
            @PathVariable Long batchId) {

        return ResponseEntity.ok(
                analyticsService.getBatchOverview(batchId));
    }

    // GET /analytics/student/{studentId}/performance
    @GetMapping("/student/{studentId}/performance")
    public ResponseEntity<StudentPerformanceDTO> getStudentPerformance(
            @PathVariable Long studentId) {

        return ResponseEntity.ok(
                analyticsService.getStudentPerformance(studentId));
    }

    // GET /analytics/modules/performance
    @GetMapping("/modules/performance")
    public ResponseEntity<List<ModulePerformanceDTO>> getModulePerformance() {

        return ResponseEntity.ok(
                analyticsService.getModulePerformance());
    }

    // GET /analytics/trainers/performance
    @GetMapping("/trainers/performance")
    public ResponseEntity<List<TrainerPerformanceDTO>> getTrainerPerformance() {

        return ResponseEntity.ok(
                analyticsService.getTrainerPerformance());
    }
 // GET /analytics/batch/{batchId}/progress
    @GetMapping("/batch/{batchId}/progress")
    public ResponseEntity<BatchProgressReportDTO> getBatchProgressReport(
            @PathVariable Long batchId) {

        return ResponseEntity.ok(
                analyticsService.getBatchProgressReport(batchId));
    }
}