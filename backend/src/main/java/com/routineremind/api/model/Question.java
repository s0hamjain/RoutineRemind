package com.routineremind.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Question(
        String id,
        String scheduleId,
        String ownerUid,
        String prompt,
        String type,
        int order
) {
}
