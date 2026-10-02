package com.marcosmoreiradev.docupodcaststudio.infrastructure.observability;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticSanitizerTest {
    @Test void redactsSecretsContentQueriesAndPersonalPaths() {
        Path project = Path.of(System.getProperty("user.home"), "projects", "private-book");
        DiagnosticSanitizer sanitizer = new DiagnosticSanitizer(project);

        String safe = sanitizer.sanitize("path=" + project + " token=abc\n"
                + "prompt=private text\nhttps://example.test/model?key=secret");

        assertFalse(safe.contains("abc"));
        assertFalse(safe.contains("private text"));
        assertFalse(safe.contains(System.getProperty("user.home")));
        assertTrue(safe.contains("<redacted>"));
        assertTrue(safe.contains("<project>") || safe.contains("<user-home>"));
    }
}
