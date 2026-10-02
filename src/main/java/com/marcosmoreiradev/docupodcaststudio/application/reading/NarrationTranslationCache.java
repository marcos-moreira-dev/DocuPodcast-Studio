package com.marcosmoreiradev.docupodcaststudio.application.reading;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/** Project-scoped cache for derived narration translations. */
public interface NarrationTranslationCache {
    NarrationTranslationCache NONE = new NarrationTranslationCache() {
        @Override public Optional<String> find(Path projectRoot, String fingerprint) {
            return Optional.empty();
        }
        @Override public void store(Path projectRoot, String fingerprint, String translatedText) { }
    };

    Optional<String> find(Path projectRoot, String fingerprint) throws IOException;
    void store(Path projectRoot, String fingerprint, String translatedText) throws IOException;

    default Optional<Failure> findFailure(Path projectRoot, String fingerprint)
            throws IOException {
        return Optional.empty();
    }

    default void storeFailure(Path projectRoot, String fingerprint, Failure failure)
            throws IOException { }

    default void clearFailure(Path projectRoot, String fingerprint) throws IOException { }

    /** Persisted diagnostic for one derived translation variant. */
    record Failure(String segmentId, String targetLanguage, String reason,
                   String rawOutput, Instant occurredAt, Map<String, String> diagnostics) {
        public Failure {
            segmentId = segmentId == null ? "" : segmentId.strip();
            targetLanguage = targetLanguage == null ? "" : targetLanguage.strip();
            reason = reason == null ? "UNKNOWN" : reason.strip();
            rawOutput = rawOutput == null ? "" : rawOutput;
            occurredAt = occurredAt == null ? Instant.now() : occurredAt;
            diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
        }
    }
}
