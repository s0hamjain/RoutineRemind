package com.routineremind.api.service;

import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.routineremind.api.model.DeviceRegistration;
import com.routineremind.api.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class DeviceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);

    private final Firestore db;

    public DeviceService(Firestore db) {
        this.db = db;
    }

    public Map<String, String> register(String uid, DeviceRegistration request) {
        if (request.token() == null || request.token().isBlank()) {
            throw ApiException.badRequest("token is required");
        }
        String deviceId = sha256(request.token());
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("token", request.token().trim());
        data.put("platform", normalizePlatform(request.platform()));
        data.put("label", request.label() == null ? "" : request.label().trim());
        data.put("updatedAt", FieldValue.serverTimestamp());
        data.put("createdAt", FieldValue.serverTimestamp());
        try {
            db.collection("devices").document(deviceId).set(data).get();
            return Map.of("deviceId", deviceId, "status", "registered");
        } catch (InterruptedException | ExecutionException e) {
            throw firestoreError(e);
        }
    }

    public List<String> tokensForUser(String uid) {
        try {
            return db.collection("devices")
                    .whereEqualTo("uid", uid)
                    .get().get().getDocuments().stream()
                    .map(snap -> snap.getString("token"))
                    .filter(token -> token != null && !token.isBlank())
                    .toList();
        } catch (InterruptedException | ExecutionException e) {
            throw firestoreError(e);
        }
    }

    private String normalizePlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            return "unknown";
        }
        return platform.trim().toLowerCase();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
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
