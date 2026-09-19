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

import com.fbs.mock_evaluation_system.dto.CourseModuleRequestDTO;
import com.fbs.mock_evaluation_system.dto.CourseModuleResponseDTO;
import com.fbs.mock_evaluation_system.service.CourseModuleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/modules")
public class CourseModuleController {

    private final CourseModuleService moduleService;

    public CourseModuleController(CourseModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @PostMapping
    public ResponseEntity<CourseModuleResponseDTO> createModule(
            @Valid @RequestBody CourseModuleRequestDTO request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(moduleService.createModule(request));
    }

    @GetMapping
    public ResponseEntity<List<CourseModuleResponseDTO>> getAllModules() {

        return ResponseEntity.ok(moduleService.getAllModules());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseModuleResponseDTO> getModuleById(
            @PathVariable Long id) {

        return ResponseEntity.ok(moduleService.getModuleById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseModuleResponseDTO> updateModule(
            @PathVariable Long id,
            @Valid @RequestBody CourseModuleRequestDTO request) {

        return ResponseEntity.ok(moduleService.updateModule(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteModule(@PathVariable Long id) {

        moduleService.deleteModule(id);

        return ResponseEntity.noContent().build();
    }
}