package com.studyos.service;

import com.studyos.dto.TaskDtos.*;
import com.studyos.model.Task;
import com.studyos.model.TaskStatus;
import com.studyos.model.User;
import com.studyos.repository.TaskRepository;
import com.studyos.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository tasks;
    private final UserRepository users;
    private final PriorityService priority;

    public TaskService(TaskRepository tasks, UserRepository users, PriorityService priority) {
        this.tasks = tasks;
        this.users = users;
        this.priority = priority;
    }

    public List<TaskResponse> list(Long userId) {
        return tasks.findByUserId(userId).stream()
                .map(t -> TaskResponse.from(t, priority.score(t)))
                .sorted(Comparator.comparingDouble(TaskResponse::priorityScore).reversed())
                .toList();
    }

    public TaskResponse create(Long userId, TaskRequest req) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Task t = Task.builder()
                .user(user)
                .title(req.title())
                .description(req.description())
                .course(req.course())
                .courseImportance(req.courseImportance())
                .dueDate(req.dueDate())
                .estimatedHours(req.estimatedHours())
                .difficulty(req.difficulty())
                .type(req.type())
                .status(req.status())
                .build();
        t = tasks.save(t);
        return TaskResponse.from(t, priority.score(t));
    }

    public TaskResponse update(Long userId, Long taskId, TaskRequest req) {
        Task t = ownedTask(userId, taskId);
        if (req.title() != null) t.setTitle(req.title());
        if (req.description() != null) t.setDescription(req.description());
        if (req.course() != null) t.setCourse(req.course());
        if (req.courseImportance() != null) t.setCourseImportance(req.courseImportance());
        if (req.dueDate() != null) t.setDueDate(req.dueDate());
        if (req.estimatedHours() != null) t.setEstimatedHours(req.estimatedHours());
        if (req.difficulty() != null) t.setDifficulty(req.difficulty());
        if (req.type() != null) t.setType(req.type());
        if (req.status() != null) {
            TaskStatus old = t.getStatus();
            t.setStatus(req.status());
            if (req.status() == TaskStatus.DONE && old != TaskStatus.DONE) {
                t.setCompletedAt(Instant.now());
            } else if (req.status() != TaskStatus.DONE) {
                t.setCompletedAt(null);
            }
        }
        t = tasks.save(t);
        return TaskResponse.from(t, priority.score(t));
    }

    public void delete(Long userId, Long taskId) {
        Task t = ownedTask(userId, taskId);
        tasks.delete(t);
    }

    private Task ownedTask(Long userId, Long taskId) {
        Task t = tasks.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!t.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return t;
    }
}
