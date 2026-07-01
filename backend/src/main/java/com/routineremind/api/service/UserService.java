package com.routineremind.api.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.routineremind.api.model.LinkedStudent;
import com.routineremind.api.model.Role;
import com.routineremind.api.model.User;
import com.routineremind.api.security.AuthUser;
import com.routineremind.api.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final String COLLECTION = "users";
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Firestore db;

    public UserService(Firestore db) {
        this.db = db;
    }

    /**
     * Creates the user doc on first login, or returns the existing profile.
     */
    public User upsertOnLogin(AuthUser authUser) {
        try {
            DocumentReference ref = db.collection(COLLECTION).document(authUser.uid());
            DocumentSnapshot snap = ref.get().get();
            if (snap.exists()) {
                return toUser(snap);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("displayName", authUser.name() != null ? authUser.name() : "");
            data.put("email", authUser.email());
            data.put("role", null);
            data.put("linkedUserIds", new ArrayList<String>());
            data.put("photoUrl", null);
            data.put("createdAt", FieldValue.serverTimestamp());
            data.put("updatedAt", FieldValue.serverTimestamp());
            ref.set(data).get();
            log.info("Created new user profile for uid={}", authUser.uid());
            return toUser(ref.get().get());
        } catch (InterruptedException | ExecutionException e) {
            throw firestoreError(e);
        }
    }

    public User require(String uid) {
        try {
            DocumentSnapshot snap = db.collection(COLLECTION).document(uid).get().get();
            if (!snap.exists()) {
                throw ApiException.notFound("User not found");
            }
            return toUser(snap);
        } catch (InterruptedException | ExecutionException e) {
            throw firestoreError(e);
        }
    }

    public User setRole(String uid, String roleValue) {
        Role role = Role.fromString(roleValue);
        if (role == null) {
            throw ApiException.badRequest("role must be 'student' or 'parent'");
        }
        try {
            DocumentReference ref = db.collection(COLLECTION).document(uid);
            Map<String, Object> update = new HashMap<>();
            update.put("role", role.wire());
            update.put("updatedAt", FieldValue.serverTimestamp());
            // A student needs a share code so a parent can link to them.
            if (role == Role.STUDENT) {
                DocumentSnapshot snap = ref.get().get();
                if (snap.getString("shareCode") == null) {
                    update.put("shareCode", generateUniqueShareCode());
                }
            }
            ref.update(update).get();
            return toUser(ref.get().get());
        } catch (InterruptedException | ExecutionException e) {
            throw firestoreError(e);
        }
    }

    /**
     * Links the calling parent to a student identified by share code.
     */
    public User linkStudent(String parentUid, String shareCode) {
        if (shareCode == null || shareCode.isBlank()) {
            throw ApiException.badRequest("shareCode is required");
        }
        try {
            DocumentReference parentRef = db.collection(COLLECTION).document(parentUid);
            DocumentSnapshot parentSnap = parentRef.get().get();
            if (!Role.PARENT.wire().equals(parentSnap.getString("role"))) {
                throw ApiException.forbidden("Only parents can link students");
            }

            List<QueryDocumentSnapshot> matches = db.collection(COLLECTION)
                    .whereEqualTo("shareCode", shareCode.trim().toUpperCase())
                    .limit(1)
                    .get().get().getDocuments();
            if (matches.isEmpty()) {
                throw ApiException.notFound("No student found for that code");
            }
            DocumentSnapshot studentSnap = matches.get(0);
            if (!Role.STUDENT.wire().equals(studentSnap.getString("role"))) {
                throw ApiException.badRequest("That code does not belong to a student account");
            }

            parentRef.update("linkedUserIds", FieldValue.arrayUnion(studentSnap.getId()),
                    "updatedAt", FieldValue.serverTimestamp()).get();
            studentSnap.getReference().update("linkedUserIds", FieldValue.arrayUnion(parentUid),
                    "updatedAt", FieldValue.serverTimestamp()).get();

            return toUser(parentRef.get().get());
        } catch (InterruptedException | ExecutionException e) {
            throw firestoreError(e);
        }
    }

    public List<LinkedStudent> linkedStudents(String parentUid) {
        try {
            User parent = require(parentUid);
            if (!Role.PARENT.wire().equals(parent.role())) {
                throw ApiException.forbidden("Only parents have linked students");
            }
            if (parent.linkedUserIds() == null || parent.linkedUserIds().isEmpty()) {
                return List.of();
            }

            List<LinkedStudent> students = new ArrayList<>();
            for (String studentUid : parent.linkedUserIds()) {
                DocumentSnapshot snap = db.collection(COLLECTION).document(studentUid).get().get();
                if (snap.exists() && Role.STUDENT.wire().equals(snap.getString("role"))) {
                    students.add(toLinkedStudent(snap));
                }
            }
            return students;
        } catch (InterruptedException | ExecutionException e) {
            throw firestoreError(e);
        }
    }

    public User unlinkStudent(String parentUid, String studentUid) {
        if (studentUid == null || studentUid.isBlank()) {
            throw ApiException.badRequest("studentUid is required");
        }
        try {
            DocumentReference parentRef = db.collection(COLLECTION).document(parentUid);
            DocumentSnapshot parentSnap = parentRef.get().get();
            if (!parentSnap.exists()) {
                throw ApiException.notFound("Parent not found");
            }
            if (!Role.PARENT.wire().equals(parentSnap.getString("role"))) {
                throw ApiException.forbidden("Only parents can unlink students");
            }
            User parent = toUser(parentSnap);
            if (!parent.linkedUserIds().contains(studentUid)) {
                throw ApiException.notFound("Student is not linked to this parent");
            }

            DocumentReference studentRef = db.collection(COLLECTION).document(studentUid);
            parentRef.update("linkedUserIds", FieldValue.arrayRemove(studentUid),
                    "updatedAt", FieldValue.serverTimestamp()).get();
            studentRef.update("linkedUserIds", FieldValue.arrayRemove(parentUid),
                    "updatedAt", FieldValue.serverTimestamp()).get();

            return toUser(parentRef.get().get());
        } catch (InterruptedException | ExecutionException e) {
            throw firestoreError(e);
        }
    }

    private String generateUniqueShareCode() throws ExecutionException, InterruptedException {
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = randomCode();
            boolean taken = !db.collection(COLLECTION)
                    .whereEqualTo("shareCode", code).limit(1)
                    .get().get().isEmpty();
            if (!taken) {
                return code;
            }
        }
        // Extremely unlikely; fall back to a longer code.
        return randomCode() + randomCode().substring(0, 2);
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private User toUser(DocumentSnapshot snap) {
        List<String> linked = (List<String>) snap.get("linkedUserIds");
        return new User(
                snap.getId(),
                snap.getString("displayName"),
                snap.getString("email"),
                snap.getString("role"),
                linked != null ? linked : List.of(),
                snap.getString("shareCode"),
                snap.getString("photoUrl")
        );
    }

    private LinkedStudent toLinkedStudent(DocumentSnapshot snap) {
        return new LinkedStudent(
                snap.getId(),
                snap.getString("displayName"),
                snap.getString("email"),
                snap.getString("shareCode"),
                snap.getString("photoUrl")
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
