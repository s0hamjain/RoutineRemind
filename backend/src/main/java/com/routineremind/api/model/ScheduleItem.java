package com.routineremind.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScheduleItem(
        String id,
        String time,
        String title,
        String description,
        String icon,
        String imageUrl,
        String parentNote,
        String audioUrl,
        String transitionHint,
        Integer sortOrder,
        boolean completed,
        String completedAt
) {
}
