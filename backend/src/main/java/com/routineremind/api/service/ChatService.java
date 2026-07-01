package com.routineremind.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.routineremind.api.config.AppProperties;
import com.routineremind.api.model.ChatMessage;
import com.routineremind.api.model.ChatResponse;
import com.routineremind.api.model.Schedule;
import com.routineremind.api.model.ScheduleItem;
import com.routineremind.api.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final String COLLECTION = "chatMessages";
    private static final int MAX_HISTORY = 50;

    private final Firestore db;
    private final ScheduleService scheduleService;
    private final AppProperties props;
    private final GoogleCredentials credentials;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ChatService(Firestore db,
                       ScheduleService scheduleService,
                       AppProperties props,
                       GoogleCredentials credentials,
                       ObjectMapper objectMapper) {
        this.db = db;
        this.scheduleService = scheduleService;
        this.props = props;
        this.credentials = credentials;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public ChatResponse answer(String uid, String ownerUid, String message) {
        if (message == null || message.isBlank()) {
            throw ApiException.badRequest("message is required");
        }

        Schedule schedule = scheduleService.getToday(uid, ownerUid);
        List<ScheduleItem> orderedItems = orderedItems(schedule);
        String createdAt = Instant.now().toString();

        Optional<GroundedAnswer> quickAnswer = deterministicAnswer(message, schedule, orderedItems);
        GroundedAnswer answer = quickAnswer.orElseGet(() -> geminiAnswer(message, schedule, orderedItems)
                .orElseGet(() -> fallbackAnswer(orderedItems)));

        ChatResponse response = new ChatResponse(
                answer.text(),
                schedule.id(),
                answer.matchedItem(),
                answer.source(),
                createdAt
        );
        saveMessage(schedule.ownerUid(), message.trim(), response);
        return response;
    }

    public List<ChatMessage> history(String uid, String ownerUid, Integer limit) {
        String studentUid = scheduleService.resolveStudentUid(uid, ownerUid);
        int safeLimit = limit == null ? 20 : Math.max(1, Math.min(MAX_HISTORY, limit));
        try {
            return db.collection(COLLECTION)
                    .whereEqualTo("ownerUid", studentUid)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(safeLimit)
                    .get().get().getDocuments().stream()
                    .map(this::toChatMessage)
                    .toList();
        } catch (InterruptedException | ExecutionException e) {
            throw firestoreError(e);
        }
    }

    private Optional<GroundedAnswer> deterministicAnswer(String message, Schedule schedule, List<ScheduleItem> items) {
        String normalized = normalize(message);
        if (items.isEmpty()) {
            return Optional.of(new GroundedAnswer("I do not see a routine for today yet. Ask your parent to add one.", null, "rules"));
        }

        ScheduleItem now = firstIncomplete(items).orElse(items.get(items.size() - 1));
        Optional<ScheduleItem> next = nextAfter(items, now);

        if (normalized.contains("what now") || normalized.contains("what do i do") || normalized.contains("what should i do")
                || normalized.contains("current") || normalized.contains("right now")) {
            return Optional.of(new GroundedAnswer(itemAnswer("Right now", now), now, "rules"));
        }
        if (normalized.contains("what next") || normalized.contains("next") || normalized.contains("after this")) {
            ScheduleItem nextItem = next.orElse(now);
            return Optional.of(new GroundedAnswer(itemAnswer(next.isPresent() ? "Next" : "You are on the last step", nextItem), nextItem, "rules"));
        }
        if (normalized.contains("done") || normalized.contains("finished") || normalized.contains("complete")) {
            return Optional.of(new GroundedAnswer("When you finish, tap Done on your schedule card. Then I can show what comes next.", now, "rules"));
        }

        for (ScheduleItem item : items) {
            String haystack = normalize(String.join(" ",
                    item.title() == null ? "" : item.title(),
                    item.description() == null ? "" : item.description(),
                    item.parentNote() == null ? "" : item.parentNote()));
            if (!haystack.isBlank() && tokens(normalized).stream().anyMatch(token -> token.length() > 3 && haystack.contains(token))) {
                return Optional.of(new GroundedAnswer(itemAnswer("I found this in your routine", item), item, "rules"));
            }
        }

        return Optional.empty();
    }

    private Optional<GroundedAnswer> geminiAnswer(String message, Schedule schedule, List<ScheduleItem> items) {
        if (!props.getAi().isEnabled()) {
            return Optional.empty();
        }
        try {
            String prompt = buildPrompt(message, schedule, items);
            String body = objectMapper.writeValueAsString(Map.of(
                    "contents", List.of(Map.of(
                            "role", "user",
                            "parts", List.of(Map.of("text", prompt))
                    )),
                    "generationConfig", Map.of(
                            "temperature", 0.2,
                            "maxOutputTokens", 120
                    )
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(vertexEndpoint()))
                    .header("Authorization", "Bearer " + accessToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Gemini request failed with status {}: {}", response.statusCode(), response.body());
                return Optional.empty();
            }

            String text = extractGeminiText(response.body());
            if (text == null || text.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new GroundedAnswer(text.trim(), firstIncomplete(items).orElse(null), "gemini"));
        } catch (IOException e) {
            log.warn("Gemini request failed", e);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private String buildPrompt(String message, Schedule schedule, List<ScheduleItem> items) {
        StringBuilder routine = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            ScheduleItem item = items.get(i);
            routine.append(i + 1)
                    .append(". time=").append(nullToBlank(item.time()))
                    .append("; title=").append(nullToBlank(item.title()))
                    .append("; description=").append(nullToBlank(item.description()))
                    .append("; parentNote=").append(nullToBlank(item.parentNote()))
                    .append("; transitionHint=").append(nullToBlank(item.transitionHint()))
                    .append("; completed=").append(item.completed())
                    .append('\n');
        }
        return """
                You are RoutineRemind, a calm assistant for a child with autism.
                Answer only using today's routine below. If the answer is not in the routine, say: "I am not sure. Let's ask your parent."
                Keep the answer under 2 short sentences. Use simple, concrete words. Do not mention policies or that you are an AI.

                Schedule title: %s
                Schedule date: %s
                Routine:
                %s
                Child question: %s
                """.formatted(schedule.title(), schedule.date(), routine, message.trim());
    }

    private String vertexEndpoint() {
        return "https://" + props.getAi().getLocation() + "-aiplatform.googleapis.com/v1/projects/"
                + props.getGcp().getProjectId()
                + "/locations/" + props.getAi().getLocation()
                + "/publishers/google/models/" + props.getAi().getModel()
                + ":generateContent";
    }

    private String accessToken() throws IOException {
        GoogleCredentials scoped = credentials.createScopedRequired()
                ? credentials.createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"))
                : credentials;
        scoped.refreshIfExpired();
        return scoped.getAccessToken().getTokenValue();
    }

    private String extractGeminiText(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            return null;
        }
        return parts.get(0).path("text").asText(null);
    }

    private GroundedAnswer fallbackAnswer(List<ScheduleItem> items) {
        return firstIncomplete(items)
                .map(item -> new GroundedAnswer(itemAnswer("Right now", item), item, "fallback"))
                .orElseGet(() -> new GroundedAnswer("Your routine looks complete. Nice work.", null, "fallback"));
    }

    private String itemAnswer(String prefix, ScheduleItem item) {
        StringBuilder answer = new StringBuilder(prefix).append(": ").append(item.title()).append(".");
        if (item.transitionHint() != null && !item.transitionHint().isBlank()) {
            answer.append(" ").append(item.transitionHint());
        } else if (item.parentNote() != null && !item.parentNote().isBlank()) {
            answer.append(" ").append(item.parentNote());
        }
        return answer.toString();
    }

    private List<ScheduleItem> orderedItems(Schedule schedule) {
        List<ScheduleItem> items = new ArrayList<>(schedule.items() == null ? List.of() : schedule.items());
        items.sort(Comparator
                .comparing((ScheduleItem item) -> item.sortOrder() == null ? Integer.MAX_VALUE : item.sortOrder())
                .thenComparing(item -> item.time() == null ? "" : item.time()));
        return items;
    }

    private Optional<ScheduleItem> firstIncomplete(List<ScheduleItem> items) {
        return items.stream().filter(item -> !item.completed()).findFirst();
    }

    private Optional<ScheduleItem> nextAfter(List<ScheduleItem> items, ScheduleItem current) {
        int index = items.indexOf(current);
        if (index >= 0 && index + 1 < items.size()) {
            return Optional.of(items.get(index + 1));
        }
        return Optional.empty();
    }

    private List<String> tokens(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split("\\s+"));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.US).replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private void saveMessage(String ownerUid, String question, ChatResponse response) {
        Map<String, Object> data = new HashMap<>();
        data.put("ownerUid", ownerUid);
        data.put("question", question);
        data.put("answer", response.answer());
        data.put("scheduleId", response.scheduleId());
        data.put("matchedItemId", response.matchedItem() == null ? null : response.matchedItem().id());
        data.put("source", response.source());
        data.put("createdAt", FieldValue.serverTimestamp());
        try {
            db.collection(COLLECTION).document().set(data).get();
        } catch (InterruptedException | ExecutionException e) {
            throw firestoreError(e);
        }
    }

    private ChatMessage toChatMessage(DocumentSnapshot snap) {
        return new ChatMessage(
                snap.getId(),
                snap.getString("ownerUid"),
                snap.getString("question"),
                snap.getString("answer"),
                snap.getString("scheduleId"),
                snap.getString("matchedItemId"),
                snap.getString("source"),
                timestampString(snap.getTimestamp("createdAt"))
        );
    }

    private String timestampString(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toDate().toInstant().toString();
    }

    private ApiException firestoreError(Exception e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        log.error("Chat Firestore operation failed", e);
        return new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                "FIRESTORE_ERROR", "Database operation failed");
    }

    private record GroundedAnswer(String text, ScheduleItem matchedItem, String source) {
    }
}
