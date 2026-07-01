package com.routineremind.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record User(
        String uid,
        String displayName,
        String email,
        String role,
        List<String> linkedUserIds,
        String shareCode,
        String photoUrl
) {
}
