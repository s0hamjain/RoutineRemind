package com.routineremind.api.service;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.routineremind.api.config.AppProperties;
import com.routineremind.api.model.MediaUpload;
import com.routineremind.api.model.Question;
import com.routineremind.api.model.QuestionResponse;
import com.routineremind.api.model.Role;
import com.routineremind.api.model.User;
import com.routineremind.api.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class QuestionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionService.class);

    private final Firestore db;
    private final Storage storage;
    private final AppProperties props;
    private final UserService userService;

    public QuestionService(Firestore db, Storage storage, AppProperties props, UserService userService) {
        this.db = db;
        this.storage = storage;
        this.props = props;
        this.userService = userService;
    }

    public List<Question> listQuestions(String uid, String scheduleId) {
        DocumentSnapshot schedule = requireScheduleForAccess(uid, scheduleId);
        try {
            return db.collection("questions")
                    .whereEqualTo("scheduleId", schedule.getId())
                    .orderBy("order", Query.Direction.ASCENDING)
                    .get().get().getDocuments().stream()
                    .map(this::toQuestion)
                    .toList();
        } catch (InterruptedException | ExecutionException e) {
            throw firestoreError(e);
        }
    }

    public MediaUpload createMediaUpload(String uid, String questionId, String contentType) {
        String normalizedContentType = normalizeContentType(contentType);
        try {
            DocumentSnapshot question = requireQuestion(questionId);
            String ownerUid = question.getString("ownerUid");
            if (!uid.equals(ownerUid)) {
                throw ApiException.forbidden("Only the student can answer this question");
            }

            String responseId = db.collection("responses").document().getId();
            String extension = extensionFor(normalizedContentType);
            String objectName = "responses/%s/%s.%s".formatted(questionId, responseId, extension);
            String mediaUrl = "gs://%s/%s".formatted(props.getGcp().getStorageBucket(), objectName);

            BlobInfo blobInfo = BlobInfo.newBuilder(props.getGcp().getStorageBucket(), objectName)
                    .setContentType(normalizedContentType)
                    .build();
            URL uploadUrl = storage.signUrl(
                    blobInfo,
                    Duration.ofMinutes(15).toMinutes(),
                    java.util.concurrent.TimeUnit.MINUTES,
                    Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                    Storage.SignUrlOption.withV4Signature()
            );

            Map<String, Object> data = new HashMap<>();
            data.put("questionId", questionId);
            data.put("studentUid", uid);
            data.put("text", null);
            data.put("mediaUrl", mediaUrl);
            data.put("contentType", normalizedContentType);
            data.put("transcript", null);
            data.put("status", "upload_pending");
            data.put("createdAt", FieldValue.serverTimestamp());
            db.collection("responses").document(responseId).set(data).get();

            return new MediaUpload(responseId, uploadUrl.toString(), objectName, mediaUrl, normalizedContentType);
        } catch (InterruptedException | ExecutionException e) {
            throw firestoreError(e);
        }
    }

    public QuestionResponse transcribeResponse(String uid, String responseId, String languageCode) {
        try {
            DocumentSnapshot response = requireResponse(responseId);
            DocumentSnapshot question = requireQuestion(response.getString("questionId"));
            authorizeQuestionAccess(uid, question.getString("ownerUid"));

            String mediaUrl = response.getString("mediaUrl");
            String contentType = response.getString("contentType");
            if (mediaUrl == null || mediaUrl.isBlank()) {
                throw ApiException.badRequest("Response does not contain media");
            }

            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setLanguageCode(languageCode == null || languageCode.isBlank() ? "en-US" : languageCode)
                    .setEncoding(encodingFor(contentType))
                    .build();
            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setUri(mediaUrl)
                    .build();

            String transcript;
            try (SpeechClient speechClient = SpeechClient.create(speechSettings())) {
                RecognizeResponse recognizeResponse = speechClient.recognize(config, audio);
                transcript = recognizeResponse.getResultsList().stream()
                        .map(SpeechRecognitionResult::getAlternativesList)
                        .filter(alternatives -> !alternatives.isEmpty())
                        .map(alternatives -> alternatives.get(0))
                        .map(SpeechRecognitionAlternative::getTranscript)
                        .filter(s -> s != null && !s.isBlank())
                        .reduce((left, right) -> left + " " + right)
                        .orElse("");
            } catch (Exception e) {
                log.error("Speech-to-Text transcription failed", e);
                throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                        "TRANSCRIPTION_FAILED", "Speech-to-Text transcription failed");
            }

            response.getReference().update(
                    "transcript", transcript,
                    "status", "transcribed",
                    "updatedAt", FieldValue.serverTimestamp()
            ).get();
            return toResponse(response.getReference().get().get());
        } catch (InterruptedException | ExecutionException e) {
            throw firestoreError(e);
        }
    }

    public Question createQuestion(String uid, String scheduleId, String prompt, String type, Integer order) {
        DocumentSnapshot schedule = requireScheduleForParentWrite(uid, scheduleId);
        if (prompt == null || prompt.isBlank()) {
            throw ApiException.badRequest("prompt is required");
        }
        try {
            DocumentReference ref = db.collection("questions").document();
            Map<String, Object> data = new HashMap<>();
            data.put("scheduleId", schedule.getId());
            data.put("ownerUid", schedule.getString("ownerUid"));
            data.put("prompt", prompt.trim());
            data.put("type", normalizeType(type));
            data.put("order", order == null ? nextQuestionOrder(schedule.getId()) : order);
            data.put("createdAt", FieldValue.serverTimestamp());
            ref.set(data).get();
            return toQuestion(ref.get().get());
        } catch (InterruptedException | ExecutionException e) {
            throw firestoreError(e);
        }
    }

    public QuestionResponse submitTextResponse(String uid, String questionId, String text) {
        if (text == null || text.isBlank()) {
            throw ApiException.badRequest("text is required");
        }
        try {
            DocumentSnapshot question = requireQuestion(questionId);
            String ownerUid = question.getString("ownerUid");
            if (!uid.equals(ownerUid)) {
                throw ApiException.forbidden("Only the student can answer this question");
            }

            DocumentReference ref = db.collection("responses").document();
            Map<String, Object> data = new HashMap<>();
            data.put("questionId", questionId);
            data.put("studentUid", uid);
            data.put("text", text.trim());
            data.put("mediaUrl", null);
            data.put("transcript", null);
            data.put("createdAt", FieldValue.serverTimestamp());
            ref.set(data).get();
            return toResponse(ref.get().get());
        } catch (InterruptedException | ExecutionException e) {
            throw firestoreError(e);
        }
    }

    public List<QuestionResponse> listResponses(String uid, String questionId) {
        DocumentSnapshot question = requireQuestion(questionId);
        authorizeQuestionAccess(uid, question.getString("ownerUid"));
        try {
            return db.collection("responses")
                    .whereEqualTo("questionId", questionId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(20)
                    .get().get().getDocuments().stream()
                    .map(this::toResponse)
                    .toList();
        } catch (InterruptedException | ExecutionException e) {
            throw firestoreError(e);
        }
    }

    private DocumentSnapshot requireScheduleForAccess(String uid, String scheduleId) {
        try {
            DocumentSnapshot schedule = db.collection("schedules").document(scheduleId).get().get();
            if (!schedule.exists()) {
                throw ApiException.notFound("Schedule not found");
            }
            authorizeQuestionAccess(uid, schedule.getString("ownerUid"));
            return schedule;
        } catch (InterruptedException | ExecutionException e) {
            throw firestoreError(e);
        }
    }

    private DocumentSnapshot requireScheduleForParentWrite(String uid, String scheduleId) {
        DocumentSnapshot schedule = requireScheduleForAccess(uid, scheduleId);
        User user = userService.require(uid);
        if (!Role.PARENT.wire().equals(user.role())) {
            throw ApiException.forbidden("Only parents can manage questions");
        }
        return schedule;
    }

    private DocumentSnapshot requireQuestion(String questionId) {
        try {
            DocumentSnapshot question = db.collection("questions").document(questionId).get().get();
            if (!question.exists()) {
                throw ApiException.notFound("Question not found");
            }
            return question;
        } catch (InterruptedException | ExecutionException e) {
            throw firestoreError(e);
        }
    }

    private DocumentSnapshot requireResponse(String responseId) throws ExecutionException, InterruptedException {
        DocumentSnapshot response = db.collection("responses").document(responseId).get().get();
        if (!response.exists()) {
            throw ApiException.notFound("Response not found");
        }
        return response;
    }

    private void authorizeQuestionAccess(String uid, String ownerUid) {
        User user = userService.require(uid);
        if (uid.equals(ownerUid)) {
            return;
        }
        if (Role.PARENT.wire().equals(user.role()) && user.linkedUserIds().contains(ownerUid)) {
            return;
        }
        throw ApiException.forbidden("You do not have access to this question");
    }

    private int nextQuestionOrder(String scheduleId) throws ExecutionException, InterruptedException {
        List<? extends DocumentSnapshot> docs = db.collection("questions")
                .whereEqualTo("scheduleId", scheduleId)
                .orderBy("order", Query.Direction.DESCENDING)
                .limit(1)
                .get().get().getDocuments();
        if (docs.isEmpty()) {
            return 1;
        }
        Long order = docs.get(0).getLong("order");
        return (order == null ? 0 : order.intValue()) + 1;
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "text";
        }
        String normalized = type.trim().toLowerCase();
        if (!List.of("text", "audio", "video").contains(normalized)) {
            throw ApiException.badRequest("type must be text, audio, or video");
        }
        return normalized;
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "audio/webm";
        }
        String normalized = contentType.trim().toLowerCase();
        if (!List.of("audio/webm", "audio/mpeg", "audio/mp3", "audio/wav", "video/webm", "video/mp4")
                .contains(normalized)) {
            throw ApiException.badRequest("Unsupported media type");
        }
        return normalized;
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "audio/mpeg", "audio/mp3" -> "mp3";
            case "audio/wav" -> "wav";
            case "video/mp4" -> "mp4";
            default -> "webm";
        };
    }

    private RecognitionConfig.AudioEncoding encodingFor(String contentType) {
        return switch (contentType == null ? "" : contentType) {
            case "audio/mpeg", "audio/mp3" -> RecognitionConfig.AudioEncoding.MP3;
            case "audio/wav" -> RecognitionConfig.AudioEncoding.LINEAR16;
            default -> RecognitionConfig.AudioEncoding.WEBM_OPUS;
        };
    }

    private SpeechSettings speechSettings() throws IOException {
        String path = props.getGcp().getServiceAccountPath();
        if (path != null && !path.isBlank() && Files.exists(Path.of(path))) {
            try (FileInputStream in = new FileInputStream(path)) {
                return SpeechSettings.newBuilder()
                        .setCredentialsProvider(FixedCredentialsProvider.create(GoogleCredentials.fromStream(in)))
                        .build();
            }
        }
        return SpeechSettings.newBuilder().build();
    }

    private Question toQuestion(DocumentSnapshot snap) {
        Long order = snap.getLong("order");
        return new Question(
                snap.getId(),
                snap.getString("scheduleId"),
                snap.getString("ownerUid"),
                snap.getString("prompt"),
                snap.getString("type"),
                order == null ? 0 : order.intValue()
        );
    }

    private QuestionResponse toResponse(DocumentSnapshot snap) {
        Timestamp createdAt = snap.getTimestamp("createdAt");
        return new QuestionResponse(
                snap.getId(),
                snap.getString("questionId"),
                snap.getString("studentUid"),
                snap.getString("text"),
                snap.getString("mediaUrl"),
                snap.getString("transcript"),
                createdAt == null ? null : createdAt.toDate().toInstant().toString()
        );
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
