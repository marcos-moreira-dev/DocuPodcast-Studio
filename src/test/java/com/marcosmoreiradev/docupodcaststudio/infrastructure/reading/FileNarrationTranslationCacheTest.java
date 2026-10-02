package com.marcosmoreiradev.docupodcaststudio.infrastructure.reading;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FileNarrationTranslationCacheTest {
    @TempDir Path projectRoot;

    @Test
    void persistsDerivedTextInsideProjectAndCanBeReopened() throws Exception {
        FileNarrationTranslationCache first = new FileNarrationTranslationCache();
        first.store(projectRoot, "fingerprint", "La derivada es continua.");

        FileNarrationTranslationCache reopened = new FileNarrationTranslationCache();
        assertEquals("La derivada es continua.",
                reopened.find(projectRoot, "fingerprint").orElseThrow());
        assertTrue(java.nio.file.Files.isRegularFile(projectRoot.resolve(
                "derived/narration-translation-v1.properties")));
    }

    @Test
    void persistsInvalidDiagnosticIndependentlyAndClearsItAfterRepair() throws Exception {
        FileNarrationTranslationCache cache = new FileNarrationTranslationCache();
        cache.storeFailure(projectRoot, "failed-fingerprint",
                new com.marcosmoreiradev.docupodcaststudio.application.reading
                        .NarrationTranslationCache.Failure(
                        "SEG-2", "en", "INVALID_OUTPUT", "Texto en español",
                        Instant.parse("2026-08-15T20:00:00Z"),
                        Map.of("rawOutput", "Texto en español")));

        var reopened = new FileNarrationTranslationCache()
                .findFailure(projectRoot, "failed-fingerprint").orElseThrow();
        assertEquals("SEG-2", reopened.segmentId());
        assertEquals("INVALID_OUTPUT", reopened.reason());
        assertEquals("Texto en español", reopened.rawOutput());
        assertTrue(java.nio.file.Files.isRegularFile(projectRoot.resolve(
                "derived/narration-translation-v1.failures.properties")));

        cache.store(projectRoot, "failed-fingerprint", "English text");
        cache.clearFailure(projectRoot, "failed-fingerprint");
        assertTrue(cache.findFailure(projectRoot, "failed-fingerprint").isEmpty());
        assertEquals("English text",
                cache.find(projectRoot, "failed-fingerprint").orElseThrow());
    }
}
