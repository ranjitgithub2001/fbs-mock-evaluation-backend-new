package com.fbs.mock_evaluation_system.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import com.fbs.mock_evaluation_system.dto.PageResponseDTO;
import com.fbs.mock_evaluation_system.dto.StudentRequestDTO;
import com.fbs.mock_evaluation_system.dto.StudentResponseDTO;
import com.fbs.mock_evaluation_system.service.StudentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponseDTO createStudent(
            @Valid @RequestBody StudentRequestDTO request) {

        return studentService.createStudent(request);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<StudentResponseDTO> createStudentsBulk(
            @Valid @RequestBody List<StudentRequestDTO> requests) {

        return studentService.createStudentsBulk(requests);
    }

    @GetMapping
    public PageResponseDTO<StudentResponseDTO> getStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction) {

        return studentService.getStudents(page, size, batchId, sortBy, direction);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponseDTO> getStudentById(
            @PathVariable Long id) {

        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @GetMapping("/by-frn")
    public StudentResponseDTO getStudentByFrn(@RequestParam String frn) {

        return studentService.getStudentByFrn(frn);
    }

    // Only name is updatable
    @PutMapping("/{id}")
    public ResponseEntity<StudentResponseDTO> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequestDTO request) {

        return ResponseEntity.ok(studentService.updateStudent(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {

        studentService.deleteStudent(id);

        return ResponseEntity.noContent().build();
    }
}