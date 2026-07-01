package com.routineremind.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatResponse(
        String answer,
        String scheduleId,
        ScheduleItem matchedItem,
        String source,
        String createdAt
) {
}
