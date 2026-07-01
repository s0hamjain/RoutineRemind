package com.routineremind.api.model;

public enum Role {
    STUDENT,
    PARENT;

    public static Role fromString(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.trim().toLowerCase()) {
            case "student" -> STUDENT;
            case "parent" -> PARENT;
            default -> null;
        };
    }

    public String wire() {
        return name().toLowerCase();
    }
}
