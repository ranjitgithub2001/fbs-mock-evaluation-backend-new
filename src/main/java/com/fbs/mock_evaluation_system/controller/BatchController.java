package com.fbs.mock_evaluation_system.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fbs.mock_evaluation_system.dto.BatchRequestDTO;
import com.fbs.mock_evaluation_system.dto.BatchResponseDTO;
import com.fbs.mock_evaluation_system.service.BatchService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/batches")
public class BatchController {

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    @PostMapping
    public ResponseEntity<BatchResponseDTO> createBatch(
            @Valid @RequestBody BatchRequestDTO requestDTO) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(batchService.createBatch(requestDTO));
    }

    @GetMapping
    public ResponseEntity<List<BatchResponseDTO>> getAllBatches() {

        return ResponseEntity.ok(batchService.getAllBatches());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BatchResponseDTO> getBatchById(
            @PathVariable Long id) {

        return ResponseEntity.ok(batchService.getBatchById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BatchResponseDTO> updateBatch(
            @PathVariable Long id,
            @Valid @RequestBody BatchRequestDTO requestDTO) {

        return ResponseEntity.ok(batchService.updateBatch(id, requestDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBatch(@PathVariable Long id) {

        batchService.deleteBatch(id);

        return ResponseEntity.noContent().build();
    }
}