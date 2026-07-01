package com.routineremind.api.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private final Firestore db;
    private final DeviceService deviceService;
    private final FirebaseMessaging firebaseMessaging;

    public ReminderService(Firestore db, DeviceService deviceService, FirebaseMessaging firebaseMessaging) {
        this.db = db;
        this.deviceService = deviceService;
        this.firebaseMessaging = firebaseMessaging;
    }

    public ReminderRunResult sendTodayReminders() {
        String today = LocalDate.now().toString();
        int schedulesChecked = 0;
        int notificationsAttempted = 0;
        int notificationsSent = 0;
        int failures = 0;

        try {
            List<DocumentSnapshot> schedules = db.collection("schedules")
                    .whereEqualTo("date", today)
                    .get().get().getDocuments().stream()
                    .map(snap -> (DocumentSnapshot) snap)
                    .toList();

            for (DocumentSnapshot schedule : schedules) {
                schedulesChecked++;
                String ownerUid = schedule.getString("ownerUid");
                if (ownerUid == null || ownerUid.isBlank() || !hasIncompleteItems(schedule)) {
                    continue;
                }

                List<String> tokens = deviceService.tokensForUser(ownerUid);
                for (String token : tokens) {
                    notificationsAttempted++;
                    try {
                        sendReminder(token, schedule);
                        notificationsSent++;
                    } catch (FirebaseMessagingException e) {
                        failures++;
                        log.warn("Failed to send FCM reminder for schedule {}: {}",
                                schedule.getId(), e.getMessage());
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failures++;
        } catch (ExecutionException e) {
            failures++;
            log.error("Reminder job failed", e);
        }

        return new ReminderRunResult(schedulesChecked, notificationsAttempted, notificationsSent, failures);
    }

    private void sendReminder(String token, DocumentSnapshot schedule) throws FirebaseMessagingException {
        String title = schedule.getString("title");
        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle("Routine reminder")
                        .setBody((title == null || title.isBlank() ? "Today's schedule" : title)
                                + " has tasks waiting.")
                        .build())
                .putData("scheduleId", schedule.getId())
                .putData("type", "schedule_reminder")
                .build();
        firebaseMessaging.send(message);
    }

    private boolean hasIncompleteItems(DocumentSnapshot schedule) {
        Object raw = schedule.get("items");
        if (!(raw instanceof List<?> items)) {
            return false;
        }
        for (Object item : items) {
            if (item instanceof Map<?, ?> map && !Boolean.TRUE.equals(map.get("completed"))) {
                return true;
            }
        }
        return false;
    }

    public record ReminderRunResult(
            int schedulesChecked,
            int notificationsAttempted,
            int notificationsSent,
            int failures
    ) {
    }
}
