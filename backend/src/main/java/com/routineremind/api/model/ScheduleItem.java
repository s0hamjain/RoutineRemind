package com.routineremind.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScheduleItem(
        String id,
        String time,
        String title,
        String description,
        boolean completed,
        String completedAt
) {
}
