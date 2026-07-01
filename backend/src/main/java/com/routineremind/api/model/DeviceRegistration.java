package com.routineremind.api.model;

public record DeviceRegistration(
        String token,
        String platform,
        String label
) {
}
