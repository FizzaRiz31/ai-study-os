package com.studyos.controller;

import com.studyos.security.AuthUtil;
import com.studyos.service.AiStudyPlanService;
import com.studyos.service.DashboardService;
import com.studyos.service.FocusSessionService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AppController {

    private final DashboardService dashboard;
    private final FocusSessionService focus;
    private final AiStudyPlanService ai;

    public AppController(DashboardService dashboard, FocusSessionService focus, AiStudyPlanService ai) {
        this.dashboard = dashboard;
        this.focus = focus;
        this.ai = ai;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        return dashboard.dashboard(AuthUtil.currentUserId());
    }

    @GetMapping("/analytics/weekly")
    public Map<String, Object> analytics() {
        return dashboard.weeklyAnalytics(AuthUtil.currentUserId());
    }

    public record FocusRequest(Long taskId, Integer minutes) {}

    @PostMapping("/focus-sessions")
    public Object logFocus(@RequestBody FocusRequest req) {
        int min = req.minutes() == null ? 25 : req.minutes();
        var s = focus.log(AuthUtil.currentUserId(), req.taskId(), min);
        return Map.of("id", s.getId(), "minutes", s.getMinutes(), "endedAt", s.getEndedAt());
    }

    public record StudyPlanRequest(Integer hoursAvailable) {}

    @PostMapping("/ai/study-plan")
    public Map<String, Object> studyPlan(@RequestBody(required = false) StudyPlanRequest req) {
        Integer hours = req == null ? null : req.hoursAvailable();
        return ai.generate(AuthUtil.currentUserId(), hours);
    }
}
