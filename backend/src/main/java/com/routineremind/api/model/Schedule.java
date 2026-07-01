package com.routineremind.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Schedule(
        String id,
        String ownerUid,
        String date,
        String title,
        String status,
        List<ScheduleItem> items
) {
}
