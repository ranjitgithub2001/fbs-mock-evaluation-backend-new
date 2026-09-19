package com.fbs.mock_evaluation_system.entity;

import java.util.List;
import jakarta.persistence.*;

@Entity
@Table(name = "batch")
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_name", nullable = false, unique = true)
    private String batchName;

    @Column(name = "batch_code", nullable = false, unique = true, length = 10)
    private String frnBatchCode;

    @OneToMany(mappedBy = "batch", fetch = FetchType.LAZY)
    private List<Student> students;

    public Batch() {}

    public Batch(String batchName, String frnBatchCode) {
        this.batchName = batchName;
        this.frnBatchCode = frnBatchCode;
    }

    public Long getId() { return id; }
    public String getBatchName() { return batchName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }
    public String getFrnBatchCode() { return frnBatchCode; }
    public void setFrnBatchCode(String frnBatchCode) { this.frnBatchCode = frnBatchCode; }
    public List<Student> getStudents() { return students; }
    public void setStudents(List<Student> students) { this.students = students; }
}