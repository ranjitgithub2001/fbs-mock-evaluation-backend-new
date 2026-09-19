package com.fbs.mock_evaluation_system.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.fbs.mock_evaluation_system.dto.BatchModuleRequestDTO;
import com.fbs.mock_evaluation_system.dto.BatchModuleResponseDTO;
import com.fbs.mock_evaluation_system.service.BatchModuleService;

import java.util.List;

@RestController
@RequestMapping("/api/batch-modules")
public class BatchModuleController {

    private final BatchModuleService batchModuleService;

    public BatchModuleController(BatchModuleService batchModuleService) {
        this.batchModuleService = batchModuleService;
    }

    @PostMapping
    public ResponseEntity<BatchModuleResponseDTO> create(
            @Valid @RequestBody BatchModuleRequestDTO request) {

        BatchModuleResponseDTO response = batchModuleService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /batch-modules          → all
    // GET /batch-modules?batchId= → filtered by batch
    @GetMapping
    public ResponseEntity<List<BatchModuleResponseDTO>> getAll(
            @RequestParam(required = false) Long batchId) {

        if (batchId != null) {
            return ResponseEntity.ok(batchModuleService.getByBatchId(batchId));
        }

        return ResponseEntity.ok(batchModuleService.getAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        batchModuleService.delete(id);

        return ResponseEntity.noContent().build();
    }
}