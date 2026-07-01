package com.routineremind.api.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.routineremind.api.model.Role;
import com.routineremind.api.model.Schedule;
import com.routineremind.api.model.ScheduleItem;
import com.routineremind.api.model.User;
import com.routineremind.api.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class ScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);
    private static final String COLLECTION = "schedules";
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final Firestore db;
    private final UserService userService;

    public ScheduleService(Firestore db, UserService userService) {
        this.db = db;
        this.userService = userService;
    }

    /**
     * Returns today's schedule for the caller (student = self; parent = first linked student).
     */
    public Schedule getToday(String uid, String ownerUid) {
        String studentUid = resolveStudentUid(uid, ownerUid);
        String today = LocalDate.now().toString();
        try {
            List<QueryDocumentSnapshot> docs = db.collection(COLLECTION)
                    .whereEqualTo("ownerUid", studentUid)
                    .whereEqualTo("date", today)
                    .limit(1)
                    .get().get().getDocuments();
            if (docs.isEmpty()) {
                throw ApiException.notFound("No schedule for today");
            }
            return toSchedule(docs.get(0));
        } catch (InterruptedException | ExecutionException e) {
            throw firestoreError(e);
        }
    }

    public List<Schedule> listSchedules(String uid, String ownerUid, String before, Integer requestedLimit) {
        String studentUid = resolveStudentUid(uid, ownerUid);
        int limit = clampLimit(requestedLimit);
        if (before != null && !before.isBlank()) {
            parseDate(before);
        }

        try {
            Query query = db.collection(COLLECTION)
                    .whereEqualTo("ownerUid", studentUid);
            if (before != null && !before.isBlank()) {
                query = query.whereLessThan("date", before);
            }
            query = query.orderBy("date", Query.Direction.DESCENDING).limit(limit);
            return query.get().get().getDocuments().stream()
                    .map(this::toSchedule)
                    .toList();
        } catch (InterruptedException | ExecutionException e) {
            throw firestoreError(e);
        }
    }

    public Schedule getSchedule(String uid, String scheduleId) {
        try {
            DocumentSnapshot snap = requireSchedule(scheduleId);
            authorizeScheduleAccess(uid, snap.getString("ownerUid"));
            return toSchedule(snap);
        } catch (InterruptedException | ExecutionException e) {
            throw firestoreError(e);
        }
    }

    public Schedule createSchedule(String uid, String ownerUid, String title, String date, List<ScheduleItem> items) {
        String studentUid = resolveStudentForParentWrite(uid, ownerUid);
        validateScheduleInput(title, date);

        try {
            DocumentReference ref = db.collection(COLLECTION).document();
            Map<String, Object> data = new HashMap<>();
            data.put("ownerUid", studentUid);
            data.put("date", date);
            data.put("title", title.trim());
            data.put("status", "upcoming");
            data.put("items", toFirestoreItems(items));
            data.put("createdAt", FieldValue.serverTimestamp());
            data.put("updatedAt", FieldValue.serverTimestamp());
            ref.set(data).get();
            return toSchedule(ref.get().get());
        } catch (InterruptedException | ExecutionException e) {
            throw firestoreError(e);
        }
    }

    public Schedule updateSchedule(String uid, String scheduleId, String title, String date, String status,
                                   List<ScheduleItem> items) {
        validateScheduleInput(title, date);
        try {
            DocumentSnapshot existing = requireSchedule(scheduleId);
            authorizeParentWrite(uid, existing.getString("ownerUid"));

            Map<String, Object> update = new HashMap<>();
            update.put("title", title.trim());
            update.put("date", date);
            if (status != null && !status.isBlank()) {
                update.put("status", normalizeStatus(status));
            }
            update.put("items", toFirestoreItems(items));
            update.put("updatedAt", FieldValue.serverTimestamp());

            existing.getReference().update(update).get();
            return toSchedule(existing.getReference().get().get());
        } catch (InterruptedException | ExecutionException e) {
            throw firestoreError(e);
        }
    }

    public Schedule completeItem(String uid, String scheduleId, String itemId) {
        if (itemId == null || itemId.isBlank()) {
            throw ApiException.badRequest("itemId is required");
        }
        try {
            DocumentSnapshot snap = requireSchedule(scheduleId);
            String ownerUid = snap.getString("ownerUid");
            if (!uid.equals(ownerUid)) {
                throw ApiException.forbidden("Only the student can complete schedule items");
            }

            List<Map<String, Object>> items = toMutableFirestoreItems(snap.get("items"));
            boolean found = false;
            for (Map<String, Object> item : items) {
                if (itemId.equals(asString(item.get("id")))) {
                    item.put("completed", true);
                    item.put("completedAt", Instant.now().toString());
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw ApiException.notFound("Schedule item not found");
            }

            String status = items.stream().allMatch(item -> Boolean.TRUE.equals(item.get("completed")))
                    ? "completed"
                    : "active";
            snap.getReference().update("items", items,
                    "status", status,
                    "updatedAt", FieldValue.serverTimestamp()).get();
            return toSchedule(snap.getReference().get().get());
        } catch (InterruptedException | ExecutionException e) {
            throw firestoreError(e);
        }
    }

    /**
     * Resolves whose schedule to show. Students see their own; parents see a linked student.
     */
    private String resolveStudentUid(String uid, String requestedOwnerUid) {
        User user = userService.require(uid);
        if (Role.PARENT.wire().equals(user.role())) {
            if (user.linkedUserIds() == null || user.linkedUserIds().isEmpty()) {
                throw ApiException.badRequest("No linked student. Link a student first.");
            }
            if (requestedOwnerUid == null || requestedOwnerUid.isBlank()) {
                return user.linkedUserIds().get(0);
            }
            if (!user.linkedUserIds().contains(requestedOwnerUid)) {
                throw ApiException.forbidden("That student is not linked to this parent");
            }
            return requestedOwnerUid;
        }
        if (requestedOwnerUid != null && !requestedOwnerUid.isBlank() && !uid.equals(requestedOwnerUid)) {
            throw ApiException.forbidden("Students can only access their own schedules");
        }
        return uid;
    }

    private String resolveStudentForParentWrite(String uid, String ownerUid) {
        User user = userService.require(uid);
        if (!Role.PARENT.wire().equals(user.role())) {
            throw ApiException.forbidden("Only parents can create or edit schedules");
        }
        return resolveStudentUid(uid, ownerUid);
    }

    private void authorizeScheduleAccess(String uid, String ownerUid) {
        resolveStudentUid(uid, ownerUid);
    }

    private void authorizeParentWrite(String uid, String ownerUid) {
        resolveStudentForParentWrite(uid, ownerUid);
    }

    private DocumentSnapshot requireSchedule(String scheduleId) throws ExecutionException, InterruptedException {
        DocumentSnapshot snap = db.collection(COLLECTION).document(scheduleId).get().get();
        if (!snap.exists()) {
            throw ApiException.notFound("Schedule not found");
        }
        return snap;
    }

    private void validateScheduleInput(String title, String date) {
        if (title == null || title.isBlank()) {
            throw ApiException.badRequest("title is required");
        }
        parseDate(date);
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            throw ApiException.badRequest("date is required");
        }
        try {
            return LocalDate.parse(date);
        } catch (RuntimeException e) {
            throw ApiException.badRequest("date must be YYYY-MM-DD");
        }
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toLowerCase();
        if (!List.of("upcoming", "active", "completed").contains(normalized)) {
            throw ApiException.badRequest("status must be upcoming, active, or completed");
        }
        return normalized;
    }

    private int clampLimit(Integer requestedLimit) {
        if (requestedLimit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(MAX_LIMIT, requestedLimit));
    }

    private List<Map<String, Object>> toFirestoreItems(List<ScheduleItem> items) {
        if (items == null) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (ScheduleItem item : items) {
            if (item.title() == null || item.title().isBlank()) {
                continue;
            }
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", item.id() == null || item.id().isBlank() ? UUID.randomUUID().toString() : item.id());
            map.put("time", item.time() == null ? "" : item.time());
            map.put("title", item.title().trim());
            map.put("description", item.description() == null ? "" : item.description());
            map.put("completed", item.completed());
            map.put("completedAt", item.completedAt());
            out.add(map);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toMutableFirestoreItems(Object rawItems) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (rawItems instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> m) {
                    items.add(new LinkedHashMap<>((Map<String, Object>) m));
                }
            }
        }
        return items;
    }

    @SuppressWarnings("unchecked")
    private Schedule toSchedule(DocumentSnapshot snap) {
        List<ScheduleItem> items = new ArrayList<>();
        Object rawItems = snap.get("items");
        if (rawItems instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> m) {
                    Map<String, Object> item = (Map<String, Object>) m;
                    items.add(new ScheduleItem(
                            asString(item.get("id")),
                            asString(item.get("time")),
                            asString(item.get("title")),
                            asString(item.get("description")),
                            Boolean.TRUE.equals(item.get("completed")),
                            asString(item.get("completedAt"))
                    ));
                }
            }
        }
        return new Schedule(
                snap.getId(),
                snap.getString("ownerUid"),
                snap.getString("date"),
                snap.getString("title"),
                snap.getString("status"),
                items
        );
    }

    private String asString(Object o) {
        return o == null ? null : o.toString();
    }

    private ApiException firestoreError(Exception e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        log.error("Firestore operation failed", e);
        return new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                "FIRESTORE_ERROR", "Database operation failed");
    }
}
