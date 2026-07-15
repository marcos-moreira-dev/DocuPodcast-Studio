package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemSourceDraft;

import java.nio.file.Path;
import java.util.List;

/** In-memory PDF region selected on the visual viewer before creating a technical problem. */
public record PdfRegionCaptureDraft(
        String id,
        int sourcePage,
        String bbox,
        Path pngPath,
        int widthPixels,
        int heightPixels,
        int dpi,
        List<String> warnings
) {
    public PdfRegionCaptureDraft {
        id = normalizeRequired(id, "id");
        sourcePage = Math.max(0, sourcePage);
        bbox = normalize(bbox);
        pngPath = pngPath == null ? null : pngPath.toAbsolutePath().normalize();
        widthPixels = Math.max(0, widthPixels);
        heightPixels = Math.max(0, heightPixels);
        dpi = Math.max(0, dpi);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public StudyProblemSourceDraft toStudySourceDraft() {
        return StudyProblemSourceDraft.visualRegion(id, "", Integer.toString(sourcePage), bbox, pngPath);
    }

    private static String normalizeRequired(String value, String field) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
