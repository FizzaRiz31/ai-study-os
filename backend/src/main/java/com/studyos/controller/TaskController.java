package com.studyos.controller;

import com.studyos.dto.TaskDtos.*;
import com.studyos.security.AuthUtil;
import com.studyos.service.TaskService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService service;
    public TaskController(TaskService service) { this.service = service; }

    @GetMapping
    public List<TaskResponse> list() {
        return service.list(AuthUtil.currentUserId());
    }

    @PostMapping
    public TaskResponse create(@RequestBody TaskRequest req) {
        return service.create(AuthUtil.currentUserId(), req);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @RequestBody TaskRequest req) {
        return service.update(AuthUtil.currentUserId(), id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(AuthUtil.currentUserId(), id);
    }
}
