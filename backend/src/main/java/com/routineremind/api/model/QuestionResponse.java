package com.routineremind.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuestionResponse(
        String id,
        String questionId,
        String studentUid,
        String text,
        String mediaUrl,
        String transcript,
        String createdAt
) {
}
