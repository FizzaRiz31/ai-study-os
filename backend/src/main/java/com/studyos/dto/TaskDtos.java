package com.studyos.dto;

import com.studyos.model.Task;
import com.studyos.model.TaskStatus;
import com.studyos.model.TaskType;

import java.time.Instant;
import java.time.LocalDate;

public class TaskDtos {

    public record TaskRequest(
        String title,
        String description,
        String course,
        Integer courseImportance,
        LocalDate dueDate,
        Double estimatedHours,
        Integer difficulty,
        TaskType type,
        TaskStatus status
    ) {}

    public record TaskResponse(
        Long id,
        String title,
        String description,
        String course,
        Integer courseImportance,
        LocalDate dueDate,
        Double estimatedHours,
        Integer difficulty,
        TaskType type,
        TaskStatus status,
        Double priorityScore,
        Instant createdAt,
        Instant completedAt
    ) {
        public static TaskResponse from(Task t, double score) {
            return new TaskResponse(
                t.getId(), t.getTitle(), t.getDescription(), t.getCourse(),
                t.getCourseImportance(), t.getDueDate(), t.getEstimatedHours(),
                t.getDifficulty(), t.getType(), t.getStatus(),
                score, t.getCreatedAt(), t.getCompletedAt()
            );
        }
    }
}
