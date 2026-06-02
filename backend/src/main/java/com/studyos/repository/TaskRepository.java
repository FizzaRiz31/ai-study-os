package com.studyos.repository;

import com.studyos.model.Task;
import com.studyos.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUserId(Long userId);
    List<Task> findByUserIdAndStatus(Long userId, TaskStatus status);
    List<Task> findByUserIdAndStatusNot(Long userId, TaskStatus status);
    List<Task> findByUserIdAndDueDateBetween(Long userId, LocalDate start, LocalDate end);
    List<Task> findByUserIdAndCompletedAtAfter(Long userId, Instant after);
}
