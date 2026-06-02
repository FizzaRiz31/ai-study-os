package com.studyos.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "tasks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Task {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    private String course;        // e.g., "CSS 342"
    private Integer courseImportance; // 1-5, user-set per course

    private LocalDate dueDate;
    private Double estimatedHours;
    private Integer difficulty;   // 1-5

    @Enumerated(EnumType.STRING)
    private TaskType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    private Instant createdAt;
    private Instant completedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = TaskStatus.BACKLOG;
        if (difficulty == null) difficulty = 3;
        if (courseImportance == null) courseImportance = 3;
        if (estimatedHours == null) estimatedHours = 1.0;
        if (type == null) type = TaskType.HOMEWORK;
    }
}
