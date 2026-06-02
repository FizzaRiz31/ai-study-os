package com.studyos.service;

import com.studyos.model.FocusSession;
import com.studyos.model.Task;
import com.studyos.repository.FocusSessionRepository;
import com.studyos.repository.TaskRepository;
import com.studyos.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class FocusSessionService {

    private final FocusSessionRepository sessions;
    private final TaskRepository tasks;
    private final UserRepository users;

    public FocusSessionService(FocusSessionRepository sessions, TaskRepository tasks, UserRepository users) {
        this.sessions = sessions;
        this.tasks = tasks;
        this.users = users;
    }

    public FocusSession log(Long userId, Long taskId, int minutes) {
        var user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Task task = null;
        if (taskId != null) {
            task = tasks.findById(taskId).orElse(null);
            if (task != null && !task.getUser().getId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }
        FocusSession s = FocusSession.builder()
                .user(user)
                .task(task)
                .minutes(minutes)
                .startedAt(Instant.now().minusSeconds(minutes * 60L))
                .endedAt(Instant.now())
                .build();
        return sessions.save(s);
    }
}
