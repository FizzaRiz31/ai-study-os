package com.studyos.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.studyos.model.Task;
import com.studyos.model.TaskStatus;
import com.studyos.model.User;
import com.studyos.repository.TaskRepository;
import com.studyos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

/**
 * Sends a tightly-scoped prompt to OpenAI with the user's open tasks and asks for
 * a structured day-by-day plan in JSON. We parse the JSON; if it fails, we retry once
 * with a "JSON ONLY" reminder. This was the most annoying part of the project — GPT
 * really wants to wrap things in markdown fences.
 */
@Service
public class AiStudyPlanService {

    private final TaskRepository tasks;
    private final UserRepository users;
    private final PriorityService priority;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RestClient http = RestClient.builder().build();

    private final String apiKey;
    private final String model;

    public AiStudyPlanService(TaskRepository tasks, UserRepository users, PriorityService priority,
                              @Value("${app.openai.api-key}") String apiKey,
                              @Value("${app.openai.model}") String model) {
        this.tasks = tasks;
        this.users = users;
        this.priority = priority;
        this.apiKey = apiKey;
        this.model = model;
    }

    public Map<String, Object> generate(Long userId, Integer hoursAvailable) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        int hours = hoursAvailable != null ? hoursAvailable
                : (user.getWeeklyStudyHoursTarget() == null ? 20 : user.getWeeklyStudyHoursTarget());

        List<Task> open = tasks.findByUserIdAndStatusNot(userId, TaskStatus.DONE);
        open.sort(Comparator.comparingDouble(priority::score).reversed());

        if (apiKey == null || apiKey.isBlank()) {
            // graceful fallback so the app still works without an API key — useful for the demo
            return fallbackPlan(open, hours);
        }

        String prompt = buildPrompt(open, hours);
        try {
            String content = callOpenAi(prompt, false);
            return parsePlan(content);
        } catch (Exception first) {
            try {
                String content = callOpenAi(prompt, true);
                return parsePlan(content);
            } catch (Exception second) {
                // last resort
                return fallbackPlan(open, hours);
            }
        }
    }

    private String buildPrompt(List<Task> open, int hoursAvailable) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a study planner. Build a 7-day plan starting ").append(LocalDate.now()).append(".\n");
        sb.append("The student has roughly ").append(hoursAvailable).append(" study hours total this week.\n");
        sb.append("Spread work across days, prioritize earlier deadlines, and don't schedule more than 4h on any single day.\n");
        sb.append("Tasks (ordered by priority):\n");
        int i = 1;
        for (Task t : open) {
            sb.append(i++).append(". \"").append(safe(t.getTitle())).append("\"");
            if (t.getCourse() != null) sb.append(" [").append(t.getCourse()).append("]");
            if (t.getDueDate() != null) sb.append(" due ").append(t.getDueDate());
            if (t.getEstimatedHours() != null) sb.append(", est ").append(t.getEstimatedHours()).append("h");
            if (t.getDifficulty() != null) sb.append(", difficulty ").append(t.getDifficulty()).append("/5");
            sb.append("\n");
            if (i > 25) break; // don't blow the context for ridiculous cases
        }
        sb.append("\nRespond ONLY with valid JSON in this exact shape, no markdown:\n");
        sb.append("{\"days\":[{\"date\":\"YYYY-MM-DD\",\"blocks\":[{\"task\":\"<title>\",\"minutes\":<int>,\"note\":\"<short tip>\"}]}]}");
        return sb.toString();
    }

    private String callOpenAi(String prompt, boolean retryStrict) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", 0.4);
        ArrayNode messages = body.putArray("messages");
        ObjectNode sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content", retryStrict
                ? "Return ONLY raw JSON. No markdown. No code fences. No commentary."
                : "You are a concise, practical study planner.");
        ObjectNode usr = messages.addObject();
        usr.put("role", "user");
        usr.put("content", prompt);

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(apiKey);

        JsonNode resp = http.post()
                .uri("https://api.openai.com/v1/chat/completions")
                .headers(hh -> hh.addAll(h))
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        return resp.path("choices").path(0).path("message").path("content").asText();
    }

    private Map<String, Object> parsePlan(String raw) throws Exception {
        String trimmed = raw == null ? "" : raw.trim();
        // strip markdown fences if GPT ignored us
        if (trimmed.startsWith("```")) {
            int firstNl = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNl >= 0 && lastFence > firstNl) {
                trimmed = trimmed.substring(firstNl + 1, lastFence).trim();
            }
        }
        JsonNode node = mapper.readTree(trimmed);
        return mapper.convertValue(node, Map.class);
    }

    private Map<String, Object> fallbackPlan(List<Task> open, int hoursAvailable) {
        // simple round-robin across 7 days
        List<Map<String, Object>> days = new ArrayList<>();
        int perDayMinutes = Math.max(30, (hoursAvailable * 60) / 7);
        int idx = 0;
        for (int d = 0; d < 7; d++) {
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", LocalDate.now().plusDays(d).toString());
            List<Map<String, Object>> blocks = new ArrayList<>();
            int remaining = perDayMinutes;
            while (remaining >= 25 && !open.isEmpty()) {
                Task t = open.get(idx % open.size());
                int slot = Math.min(remaining, 50);
                Map<String, Object> b = new LinkedHashMap<>();
                b.put("task", t.getTitle());
                b.put("minutes", slot);
                b.put("note", "Auto-scheduled (no AI key configured)");
                blocks.add(b);
                remaining -= slot;
                idx++;
            }
            day.put("blocks", blocks);
            days.add(day);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("days", days);
        out.put("source", "fallback");
        return out;
    }

    private String safe(String s) { return s == null ? "" : s.replace("\"", "'"); }
}
