package com.marcosmoreiradev.docupodcaststudio.application.narrative;

import java.util.List;

/** Text-only narrative context compiled exclusively from the imported document. */
public record NarrativeDocumentContext(
        String documentFingerprint,
        String normalizedDocumentText,
        String blockId,
        String paragraphText,
        String promptContext,
        List<String> sourceBlockIds
) {
    public NarrativeDocumentContext {
        documentFingerprint = safe(documentFingerprint);
        normalizedDocumentText = safe(normalizedDocumentText);
        blockId = safe(blockId);
        paragraphText = safe(paragraphText);
        promptContext = safe(promptContext);
        sourceBlockIds = sourceBlockIds == null ? List.of() : List.copyOf(sourceBlockIds);
    }

    private static String safe(String value) {
        return value == null ? "" : value.strip();
    }
}
