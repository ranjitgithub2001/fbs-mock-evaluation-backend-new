package com.fbs.mock_evaluation_system.controller;

import com.fbs.mock_evaluation_system.dto.StudentReportEmailRequestDTO;
import com.fbs.mock_evaluation_system.service.StudentReportService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class StudentReportController {

    private final StudentReportService studentReportService;

    public StudentReportController(StudentReportService studentReportService) {
        this.studentReportService = studentReportService;
    }

    @PostMapping("/student/email")
    public ResponseEntity<Void> sendStudentReport(
            @Valid @RequestBody StudentReportEmailRequestDTO request) {

        studentReportService.sendReport(request);
        return ResponseEntity.ok().build();
    }
}