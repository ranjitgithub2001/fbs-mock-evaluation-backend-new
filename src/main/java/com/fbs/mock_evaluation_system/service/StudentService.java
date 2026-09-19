package com.fbs.mock_evaluation_system.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fbs.mock_evaluation_system.dto.PageResponseDTO;
import com.fbs.mock_evaluation_system.dto.StudentRequestDTO;
import com.fbs.mock_evaluation_system.dto.StudentResponseDTO;
import com.fbs.mock_evaluation_system.entity.Batch;
import com.fbs.mock_evaluation_system.entity.Student;
import com.fbs.mock_evaluation_system.exception.BatchNotFoundException;
import com.fbs.mock_evaluation_system.exception.InvalidInputException;
import com.fbs.mock_evaluation_system.exception.ResourceNotFoundException;
import com.fbs.mock_evaluation_system.mapper.StudentMapper;
import com.fbs.mock_evaluation_system.repository.BatchRepository;
import com.fbs.mock_evaluation_system.repository.StudentRepository;
import com.fbs.mock_evaluation_system.util.FrnParser;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final BatchRepository batchRepository;

    public StudentService(StudentRepository studentRepository,
            BatchRepository batchRepository) {
        this.studentRepository = studentRepository;
        this.batchRepository = batchRepository;
    }

    @Transactional
    public StudentResponseDTO createStudent(StudentRequestDTO request) {

        String normalizedFrn = request.getFrn().trim().toUpperCase();
        String normalizedName = request.getName().trim();

        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new BatchNotFoundException(
                        "Batch not found with id: " + request.getBatchId()));

        String frnMiddleBlock = FrnParser.extractMiddleBlock(normalizedFrn);
        if (!frnMiddleBlock.equals(batch.getFrnBatchCode())) {
            throw new InvalidInputException("FRN does not belong to the selected batch");
        }

        String rollNumber = FrnParser.extractRollNumber(normalizedFrn);

        Student student = StudentMapper.toEntity(normalizedFrn, normalizedName, rollNumber, batch);

        try {
            return StudentMapper.toDTO(studentRepository.save(student));
        } catch (DataIntegrityViolationException ex) {
            throw new InvalidInputException("Student with this FRN already exists");
        }
    }

    @Transactional
    public List<StudentResponseDTO> createStudentsBulk(List<StudentRequestDTO> requests) {

        if (requests == null || requests.isEmpty()) {
            throw new InvalidInputException("Student list cannot be empty");
        }

        List<Long> batchIds = new ArrayList<>();
        for (StudentRequestDTO request : requests) {
            if (!batchIds.contains(request.getBatchId())) {
                batchIds.add(request.getBatchId());
            }
        }

        List<Batch> batches = batchRepository.findAllById(batchIds);
        Map<Long, Batch> batchMap = new HashMap<>();
        for (Batch batch : batches) {
            batchMap.put(batch.getId(), batch);
        }

        List<Student> studentsToSave = new ArrayList<>();

        for (StudentRequestDTO request : requests) {
            String normalizedFrn = request.getFrn().trim().toUpperCase();
            String normalizedName = request.getName().trim();
            Batch batch = batchMap.get(request.getBatchId());

            if (batch == null) {
                throw new BatchNotFoundException("Batch not found with id: " + request.getBatchId());
            }

            String frnMiddleBlock = FrnParser.extractMiddleBlock(normalizedFrn);
            if (!frnMiddleBlock.equals(batch.getFrnBatchCode())) {
                throw new InvalidInputException(
                        "FRN does not belong to the selected batch: " + normalizedFrn);
            }

            String rollNumber = FrnParser.extractRollNumber(normalizedFrn);
            studentsToSave.add(StudentMapper.toEntity(normalizedFrn, normalizedName, rollNumber, batch));
        }

        try {
            List<Student> savedStudents = studentRepository.saveAll(studentsToSave);
            List<StudentResponseDTO> response = new ArrayList<>();
            for (Student student : savedStudents) {
                response.add(StudentMapper.toDTO(student));
            }
            return response;
        } catch (DataIntegrityViolationException ex) {
            throw new InvalidInputException("Duplicate FRN or roll number detected");
        }
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<StudentResponseDTO> getStudents(
            int page, int size, Long batchId, String sortBy, String direction) {

        if (page < 0) throw new InvalidInputException("Page number must be zero or positive");
        if (size <= 0) throw new InvalidInputException("Page size must be greater than zero");
        if (size > 50) size = 50;

        Sort sort = Sort.unsorted();
        if (sortBy != null) {
            if (!sortBy.equals("name") && !sortBy.equals("frn") && !sortBy.equals("rollNumber")) {
                throw new InvalidInputException("Invalid sort field: " + sortBy);
            }
            if (direction == null || direction.equalsIgnoreCase("asc")) {
                sort = Sort.by(sortBy).ascending();
            } else if (direction.equalsIgnoreCase("desc")) {
                sort = Sort.by(sortBy).descending();
            } else {
                throw new InvalidInputException("Direction must be 'asc' or 'desc'");
            }
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Student> studentPage;

        if (batchId != null) {
            batchRepository.findById(batchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + batchId));
            // Only show active students
            studentPage = studentRepository.findByBatchIdAndActiveTrue(batchId, pageable);
        } else {
            // Only show active students
            studentPage = studentRepository.findByActiveTrue(pageable);
        }

        List<StudentResponseDTO> response = new ArrayList<>();
        for (Student student : studentPage.getContent()) {
            response.add(StudentMapper.toDTO(student));
        }

        return new PageResponseDTO<>(
                response,
                studentPage.getNumber(),
                studentPage.getSize(),
                studentPage.getTotalElements(),
                studentPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public StudentResponseDTO getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
        return StudentMapper.toDTO(student);
    }

    @Transactional(readOnly = true)
    public StudentResponseDTO getStudentByFrn(String frn) {
        if (frn == null || frn.trim().isEmpty()) {
            throw new InvalidInputException("FRN cannot be empty");
        }
        Student student = studentRepository.findByFrnAndActiveTrue(frn.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with FRN: " + frn));
        return StudentMapper.toDTO(student);
    }

    // Only name is updatable — FRN and batch are immutable identity fields
    @Transactional
    public StudentResponseDTO updateStudent(Long id, StudentRequestDTO request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
        student.setName(request.getName().trim());
        return StudentMapper.toDTO(studentRepository.save(student));
    }

    // ── Deletion blocked — students are managed by Student Management System ──
    @Transactional
    public void deleteStudent(Long id) {
        throw new InvalidInputException(
            "Students cannot be deleted from the Mock Evaluation System. " +
            "Please manage students through the FBS Student Management System.");
    }
}