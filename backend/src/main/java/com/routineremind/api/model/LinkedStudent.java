package com.routineremind.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LinkedStudent(
        String uid,
        String displayName,
        String email,
        String shareCode,
        String photoUrl
) {
}
