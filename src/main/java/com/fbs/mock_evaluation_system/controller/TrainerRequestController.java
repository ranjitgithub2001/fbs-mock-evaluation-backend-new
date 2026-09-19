package com.fbs.mock_evaluation_system.controller;

import com.fbs.mock_evaluation_system.dto.TrainerRequestDTO;
import com.fbs.mock_evaluation_system.dto.TrainerRequestResponseDTO;
import com.fbs.mock_evaluation_system.entity.TrainerRequestStatus;
import com.fbs.mock_evaluation_system.service.TrainerRequestService;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trainer-requests")
public class TrainerRequestController {

    private final TrainerRequestService trainerRequestService;

    public TrainerRequestController(TrainerRequestService trainerRequestService) {
        this.trainerRequestService = trainerRequestService;
    }

    // POST /trainer-requests — public, no auth needed
    @PostMapping
    public ResponseEntity<TrainerRequestResponseDTO> submitRequest(
            @Valid @RequestBody TrainerRequestDTO request) {

        TrainerRequestResponseDTO response =
                trainerRequestService.submitRequest(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // GET /trainer-requests — admin only
    @GetMapping
    public ResponseEntity<Page<TrainerRequestResponseDTO>> getRequests(
            @RequestParam(required = false) TrainerRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "requestedAt"));

        Page<TrainerRequestResponseDTO> response =
                trainerRequestService.getRequests(status, pageable);

        return ResponseEntity.ok(response);
    }

    // GET /trainer-requests/{id} — admin only
    @GetMapping("/{id}")
    public ResponseEntity<TrainerRequestResponseDTO> getRequestById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                trainerRequestService.getRequestById(id));
    }

    // POST /trainer-requests/{id}/approve — admin only
    @PostMapping("/{id}/approve")
    public ResponseEntity<TrainerRequestResponseDTO> approveRequest(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                trainerRequestService.approveRequest(id));
    }

    // POST /trainer-requests/{id}/reject — admin only
    @PostMapping("/{id}/reject")
    public ResponseEntity<TrainerRequestResponseDTO> rejectRequest(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {

        return ResponseEntity.ok(
                trainerRequestService.rejectRequest(id, reason));
    }
}