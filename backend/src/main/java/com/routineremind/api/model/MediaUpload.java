package com.routineremind.api.model;

public record MediaUpload(
        String responseId,
        String uploadUrl,
        String objectName,
        String mediaUrl,
        String contentType
) {
}
