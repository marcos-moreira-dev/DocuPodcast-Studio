package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingScope;

import java.util.List;
import java.util.Objects;

/**
 * Passive authority for the three independent stages of document readiness.
 * Keeps semantic/listening preparation separate from concrete audio coverage.
 */
public record DocumentReadingReadinessSnapshot(
        DocumentProcessingScope scope,
        boolean semanticReady,
        boolean listeningReady,
        String listeningLanguage,
        List<String> missingTranslationSegmentIds,
        List<String> invalidTranslationSegmentIds,
        int audioReusableCount,
        int audioMissingCount,
        int audioStaleCount,
        int audioInvalidCount) {

    public DocumentReadingReadinessSnapshot {
        scope = Objects.requireNonNullElse(scope, DocumentProcessingScope.FULL_DOCUMENT);
        listeningLanguage = Objects.requireNonNullElse(listeningLanguage, "").strip();
        missingTranslationSegmentIds = copy(missingTranslationSegmentIds);
        invalidTranslationSegmentIds = copy(invalidTranslationSegmentIds);
        audioReusableCount = Math.max(0, audioReusableCount);
        audioMissingCount = Math.max(0, audioMissingCount);
        audioStaleCount = Math.max(0, audioStaleCount);
        audioInvalidCount = Math.max(0, audioInvalidCount);
        listeningReady = semanticReady && listeningReady
                && missingTranslationSegmentIds.isEmpty()
                && invalidTranslationSegmentIds.isEmpty();
    }

    public static DocumentReadingReadinessSnapshot unavailable(String language) {
        return new DocumentReadingReadinessSnapshot(
                DocumentProcessingScope.FULL_DOCUMENT, false, false, language,
                List.of(), List.of(), 0, 0, 0, 0);
    }

    public boolean audioReady() {
        return listeningReady && audioMissingCount == 0
                && audioStaleCount == 0 && audioInvalidCount == 0;
    }

    public boolean reprocessing() {
        return semanticReady && listeningReady && audioReady();
    }

    public String completeReadingActionLabel() {
        if (reprocessing()) {
            return "Reprocesar lectura completa";
        }
        if (semanticReady && listeningReady) {
            return "Completar audio de lectura";
        }
        return "Procesar lectura completa";
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(Objects::nonNull).map(String::strip)
                .filter(value -> !value.isBlank()).distinct().toList();
    }
}
