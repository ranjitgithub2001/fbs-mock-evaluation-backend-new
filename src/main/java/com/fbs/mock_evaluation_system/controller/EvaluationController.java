package com.fbs.mock_evaluation_system.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fbs.mock_evaluation_system.dto.EvaluationFilterDTO;
import com.fbs.mock_evaluation_system.dto.EvaluationRequestDTO;
import com.fbs.mock_evaluation_system.dto.EvaluationResponseDTO;
import com.fbs.mock_evaluation_system.dto.EvaluationUpdateRequestDTO;
import com.fbs.mock_evaluation_system.dto.SuggestedStageDTO;
import com.fbs.mock_evaluation_system.entity.EvaluationResult;
import com.fbs.mock_evaluation_system.exception.InvalidInputException;
import com.fbs.mock_evaluation_system.service.EvaluationService;

import jakarta.validation.Valid;

// FIX #5 — removed duplicate import of java.util.List

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping
    public ResponseEntity<EvaluationResponseDTO> create(
            @Valid @RequestBody EvaluationRequestDTO request) {

        EvaluationResponseDTO response = evaluationService.create(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<EvaluationResponseDTO>> getByStudent(
            @PathVariable Long studentId) {

        List<EvaluationResponseDTO> response = evaluationService.getByStudent(studentId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EvaluationResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody EvaluationUpdateRequestDTO request) {

        EvaluationResponseDTO response = evaluationService.update(id, request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/batch-module/{batchModuleId}")
    public ResponseEntity<List<EvaluationResponseDTO>> getByBatchModule(
            @PathVariable Long batchModuleId) {

        List<EvaluationResponseDTO> evaluations =
                evaluationService.getEvaluationsByBatchModule(batchModuleId);

        return ResponseEntity.ok(evaluations);
    }

    @GetMapping
    public ResponseEntity<Page<EvaluationResponseDTO>> getAllEvaluations(

            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long batchModuleId,
            @RequestParam(required = false) Long trainerId,
            @RequestParam(required = false) Long mockStageId,
            @RequestParam(required = false) EvaluationResult finalResult,
            @RequestParam(required = false) LocalDate mockDateFrom,
            @RequestParam(required = false) LocalDate mockDateTo,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "mockDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        // FIX #15 — cap page size
        if (size > 50) {
            size = 50;
        }

        List<String> allowedSortFields = List.of("mockDate", "attemptNumber", "finalResult");
        if (!allowedSortFields.contains(sortBy)) {
            throw new InvalidInputException(
                    "Invalid sortBy field: " + sortBy +
                    ". Allowed values: mockDate, attemptNumber, finalResult");
        }

        Sort.Direction direction = sortDir.equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        EvaluationFilterDTO filter = new EvaluationFilterDTO();
        filter.setStudentId(studentId);
        filter.setBatchModuleId(batchModuleId);
        filter.setTrainerId(trainerId);
        filter.setMockStageId(mockStageId);
        filter.setFinalResult(finalResult);
        filter.setMockDateFrom(mockDateFrom);
        filter.setMockDateTo(mockDateTo);

        Page<EvaluationResponseDTO> result = evaluationService.getEvaluations(filter, pageable);

        return ResponseEntity.ok(result);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        evaluationService.delete(id);

        return ResponseEntity.noContent().build();
    }
    @GetMapping("/suggest-stage")
    public ResponseEntity<SuggestedStageDTO> suggestNextStage(
            @RequestParam Long studentId,
            @RequestParam Long batchModuleId) {

        return ResponseEntity.ok(evaluationService.suggestNextStage(studentId, batchModuleId));
    }
}