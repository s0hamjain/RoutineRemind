package com.routineremind.api;

import com.routineremind.api.model.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Lightweight unit tests. The full Spring context is intentionally not loaded here
 * because it initializes Firebase (requiring live credentials); context wiring is
 * verified at runtime / via integration tests configured separately.
 */
class ApiApplicationTests {

    @Test
    void roleParsingIsCaseInsensitive() {
        assertEquals(Role.STUDENT, Role.fromString("Student"));
        assertEquals(Role.PARENT, Role.fromString("PARENT"));
        assertNull(Role.fromString("teacher"));
    }

    @Test
    void roleWireFormatIsLowercase() {
        assertEquals("student", Role.STUDENT.wire());
        assertEquals("parent", Role.PARENT.wire());
    }
}
