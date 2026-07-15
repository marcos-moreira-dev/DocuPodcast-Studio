package com.marcosmoreiradev.docupodcaststudio.application.process;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExternalProcessRequestTest {
    @Test
    void requiresNonBlankCommandAndKeepsAuditLabel() {
        assertThrows(IllegalArgumentException.class, () -> ExternalProcessRequest.of(List.of(""), "bad", Duration.ofSeconds(1)));
        ExternalProcessRequest request = new ExternalProcessRequest(List.of("python", "script.py"), null,
                Duration.ofSeconds(5), Map.of("PYTHONUNBUFFERED", "1"), "xtts", false);
        assertEquals("xtts", request.auditLabel());
        assertTrue(request.commandAudit().contains("python"));
        assertEquals("1", request.environment().get("PYTHONUNBUFFERED"));
    }
}
