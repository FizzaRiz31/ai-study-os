package com.studyos.service;

import com.studyos.model.Task;
import com.studyos.model.TaskStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Smart Priority Score.
 *
 * The score answers a simple question: "of everything I haven't finished, what should I do next?"
 *
 * Five inputs, each normalized into the 0-1 range, then weighted and summed to a 0-100 score:
 *   - deadline urgency  (how close is it due, with overdue capped high)
 *   - difficulty        (1-5)
 *   - estimated hours   (capped at 10h so a single huge task doesn't dominate forever)
 *   - course importance (1-5, user sets per course)
 *   - overdue penalty   (flat bump if past due and not done)
 *
 * Done tasks score 0 so they fall to the bottom of any sorted list.
 *
 * Weights were tuned by hand on my own homework for a couple weeks. Deadline carries the most
 * weight because that's what wakes me up at 2am; difficulty matters less because I tend to
 * overestimate it. Easy to tweak — they're constants at the top of the file.
 */
@Service
public class PriorityService {

    private static final double W_DEADLINE  = 0.40;
    private static final double W_DIFFICULTY = 0.15;
    private static final double W_HOURS     = 0.15;
    private static final double W_COURSE    = 0.15;
    private static final double W_OVERDUE   = 0.15;

    public double score(Task t) {
        if (t.getStatus() == TaskStatus.DONE) return 0.0;

        double deadline   = deadlineUrgency(t.getDueDate());
        double difficulty = norm(t.getDifficulty(), 1, 5);
        double hours      = Math.min(t.getEstimatedHours() == null ? 1.0 : t.getEstimatedHours(), 10.0) / 10.0;
        double course     = norm(t.getCourseImportance(), 1, 5);
        double overdue    = isOverdue(t) ? 1.0 : 0.0;

        double raw = W_DEADLINE  * deadline
                   + W_DIFFICULTY * difficulty
                   + W_HOURS     * hours
                   + W_COURSE    * course
                   + W_OVERDUE   * overdue;

        return Math.round(raw * 1000.0) / 10.0; // 0-100, one decimal
    }

    /** Closer = higher. Past due saturates at 1.0. Beyond 14 days, near 0. */
    private double deadlineUrgency(LocalDate due) {
        if (due == null) return 0.2; // unknown due date -> mild urgency
        long days = ChronoUnit.DAYS.between(LocalDate.now(), due);
        if (days <= 0) return 1.0;
        if (days >= 14) return 0.05;
        // smooth-ish decay: 1 day -> ~0.93, 7 days -> ~0.5, 14 days -> ~0.05
        return Math.max(0.05, 1.0 - (days / 14.0));
    }

    private boolean isOverdue(Task t) {
        return t.getDueDate() != null
            && t.getDueDate().isBefore(LocalDate.now())
            && t.getStatus() != TaskStatus.DONE;
    }

    private double norm(Integer v, int min, int max) {
        if (v == null) return 0.5;
        int clamped = Math.max(min, Math.min(max, v));
        return (clamped - min) / (double) (max - min);
    }
}
