package com.routineremind.api.model;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        String ownerUid,
        @NotBlank String message
) {
}
