package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentSourceCapabilities;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;

import java.util.List;
import java.util.Objects;

/** Derived Estudio documental view: source, prepared reading, support items and audio state. */
public record DocumentStudyProjection(
        String title,
        SourceDocumentFormat sourceFormat,
        DocumentSourceCapabilities sourceCapabilities,
        int primaryFragmentCount,
        int narratableFragmentCount,
        int secondaryUnitCount,
        int sourceVisualCount,
        int tableCount,
        int imageCount,
        int mathCount,
        int importIssueCount,
        int audioReadyCount,
        int audioMissingCount,
        DocumentStudyReadiness readiness,
        List<DocumentStudySupportItem> supportItems,
        List<String> importIssueCodes
) {
    public DocumentStudyProjection {
        title = title == null || title.isBlank() ? "Estudio documental" : title.strip();
        sourceFormat = Objects.requireNonNullElse(sourceFormat, SourceDocumentFormat.UNKNOWN);
        sourceCapabilities = sourceCapabilities == null
                ? DocumentSourceCapabilities.forFormat(sourceFormat)
                : sourceCapabilities;
        primaryFragmentCount = Math.max(0, primaryFragmentCount);
        narratableFragmentCount = Math.max(0, narratableFragmentCount);
        secondaryUnitCount = Math.max(0, secondaryUnitCount);
        sourceVisualCount = Math.max(0, sourceVisualCount);
        tableCount = Math.max(0, tableCount);
        imageCount = Math.max(0, imageCount);
        mathCount = Math.max(0, mathCount);
        importIssueCount = Math.max(0, importIssueCount);
        audioReadyCount = Math.max(0, audioReadyCount);
        audioMissingCount = Math.max(0, audioMissingCount);
        readiness = readiness == null
                ? new DocumentStudyReadiness(false, false, false, false, List.of("Abre una fuente documental."))
                : readiness;
        supportItems = supportItems == null ? List.of() : List.copyOf(supportItems);
        importIssueCodes = importIssueCodes == null ? List.of() : List.copyOf(importIssueCodes);
    }
}
