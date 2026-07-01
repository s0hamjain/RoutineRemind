package com.routineremind.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessage(
        String id,
        String ownerUid,
        String question,
        String answer,
        String scheduleId,
        String matchedItemId,
        String source,
        String createdAt
) {
}
