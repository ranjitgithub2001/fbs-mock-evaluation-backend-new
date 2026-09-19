package com.fbs.mock_evaluation_system.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "student",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"batch_id", "roll_number"})
    }
)
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "frn", nullable = false, unique = true)
    private String frn;

    @Column(name = "full_name", nullable = false)
    private String name;

    @Column(name = "roll_number")
    private String rollNumber;

    @Column(name = "is_active")
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    public Student() {}

    public Student(String frn, String name, String rollNumber, Batch batch) {
        this.frn = frn;
        this.name = name;
        this.rollNumber = rollNumber;
        this.batch = batch;
    }

    public Long getId() { return id; }
    public String getFrn() { return frn; }
    public void setFrn(String frn) { this.frn = frn; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Batch getBatch() { return batch; }
    public void setBatch(Batch batch) { this.batch = batch; }
}