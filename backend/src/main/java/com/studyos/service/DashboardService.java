package com.studyos.service;

import com.studyos.dto.TaskDtos.TaskResponse;
import com.studyos.model.FocusSession;
import com.studyos.model.Task;
import com.studyos.model.TaskStatus;
import com.studyos.repository.FocusSessionRepository;
import com.studyos.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final TaskRepository tasks;
    private final FocusSessionRepository focus;
    private final PriorityService priority;

    public DashboardService(TaskRepository tasks, FocusSessionRepository focus, PriorityService priority) {
        this.tasks = tasks;
        this.focus = focus;
        this.priority = priority;
    }

    public Map<String, Object> dashboard(Long userId) {
        List<Task> all = tasks.findByUserId(userId);
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(7);

        List<TaskResponse> top = all.stream()
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .map(t -> TaskResponse.from(t, priority.score(t)))
                .sorted(Comparator.comparingDouble(TaskResponse::priorityScore).reversed())
                .limit(5)
                .toList();

        List<TaskResponse> upcoming = all.stream()
                .filter(t -> t.getDueDate() != null
                        && !t.getDueDate().isBefore(today)
                        && !t.getDueDate().isAfter(weekEnd)
                        && t.getStatus() != TaskStatus.DONE)
                .map(t -> TaskResponse.from(t, priority.score(t)))
                .sorted(Comparator.comparing(TaskResponse::dueDate))
                .toList();

        long overdue = all.stream()
                .filter(t -> t.getStatus() != TaskStatus.DONE
                        && t.getDueDate() != null
                        && t.getDueDate().isBefore(today))
                .count();

        double weeklyHours = all.stream()
                .filter(t -> t.getDueDate() != null
                        && !t.getDueDate().isBefore(today)
                        && !t.getDueDate().isAfter(weekEnd)
                        && t.getStatus() != TaskStatus.DONE)
                .mapToDouble(t -> t.getEstimatedHours() == null ? 0 : t.getEstimatedHours())
                .sum();

        Instant weekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        int focusMinutes = focus.findByUserIdAndEndedAtAfter(userId, weekAgo).stream()
                .mapToInt(s -> s.getMinutes() == null ? 0 : s.getMinutes())
                .sum();

        // workload risk
        String workloadRisk = computeRisk(weeklyHours, overdue, focusMinutes);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("topPriorities", top);
        out.put("upcoming", upcoming);
        out.put("overdueCount", overdue);
        out.put("weeklyEstimatedHours", round(weeklyHours));
        out.put("focusMinutesLast7Days", focusMinutes);
        out.put("workloadRisk", workloadRisk);
        return out;
    }

    public Map<String, Object> weeklyAnalytics(Long userId) {
        Instant weekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        List<FocusSession> sessions = focus.findByUserIdAndEndedAtAfter(userId, weekAgo);
        List<Task> completed = tasks.findByUserIdAndCompletedAtAfter(userId, weekAgo);

        // group focus minutes by day
        Map<String, Integer> byDay = new TreeMap<>();
        for (int i = 6; i >= 0; i--) {
            byDay.put(LocalDate.now().minusDays(i).toString(), 0);
        }
        for (FocusSession s : sessions) {
            String day = s.getEndedAt().atZone(ZoneId.systemDefault()).toLocalDate().toString();
            byDay.merge(day, s.getMinutes() == null ? 0 : s.getMinutes(), Integer::sum);
        }

        // top course by estimated hours of completed work
        Map<String, Double> hoursByCourse = completed.stream()
                .filter(t -> t.getCourse() != null)
                .collect(Collectors.groupingBy(
                        Task::getCourse,
                        Collectors.summingDouble(t -> t.getEstimatedHours() == null ? 0 : t.getEstimatedHours())));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("focusMinutesByDay", byDay);
        out.put("completedTaskCount", completed.size());
        out.put("hoursByCourse", hoursByCourse);
        return out;
    }

    private String computeRisk(double weeklyHours, long overdue, int focusMinutes) {
        if (weeklyHours > 20 && overdue >= 3 && focusMinutes < 120) {
            return "HIGH: Your workload looks heavy this week. Consider moving low-priority tasks or breaking large assignments into smaller sessions.";
        }
        if (weeklyHours > 15 || overdue >= 2) {
            return "MEDIUM: Busy week ahead. Plan focus blocks early.";
        }
        return "LOW: Looking manageable.";
    }

    private double round(double v) { return Math.round(v * 10.0) / 10.0; }
}
