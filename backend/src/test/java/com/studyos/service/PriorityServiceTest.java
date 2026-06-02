package com.studyos.service;

import com.studyos.model.Task;
import com.studyos.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PriorityServiceTest {

    private final PriorityService svc = new PriorityService();

    @Test
    void doneTaskScoresZero() {
        Task t = baseTask();
        t.setStatus(TaskStatus.DONE);
        assertEquals(0.0, svc.score(t));
    }

    @Test
    void overdueScoresHigherThanFuture() {
        Task overdue = baseTask();
        overdue.setDueDate(LocalDate.now().minusDays(1));

        Task future = baseTask();
        future.setDueDate(LocalDate.now().plusDays(10));

        assertTrue(svc.score(overdue) > svc.score(future));
    }

    @Test
    void higherDifficultyRaisesScore() {
        Task easy = baseTask();
        easy.setDifficulty(1);
        Task hard = baseTask();
        hard.setDifficulty(5);
        assertTrue(svc.score(hard) > svc.score(easy));
    }

    private Task baseTask() {
        Task t = new Task();
        t.setStatus(TaskStatus.BACKLOG);
        t.setDifficulty(3);
        t.setCourseImportance(3);
        t.setEstimatedHours(2.0);
        t.setDueDate(LocalDate.now().plusDays(3));
        return t;
    }
}
