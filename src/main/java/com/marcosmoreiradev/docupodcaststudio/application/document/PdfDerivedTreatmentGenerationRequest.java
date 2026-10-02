package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentKind;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Explicit opt-in request for a local derivative; opening a page never creates one. */
public record PdfDerivedTreatmentGenerationRequest(
        Path projectRoot,
        int pageNumber,
        PdfDerivedTreatmentKind kind,
        List<String> sourceRegionIds,
        String engineId,
        boolean explicitlyEnabled,
        Map<String, String> options
) {
    public PdfDerivedTreatmentGenerationRequest {
        projectRoot = Objects.requireNonNull(projectRoot, "projectRoot").toAbsolutePath().normalize();
        if (pageNumber <= 0) throw new IllegalArgumentException("pageNumber must be positive");
        kind = Objects.requireNonNull(kind, "kind");
        sourceRegionIds = sourceRegionIds == null ? List.of() : sourceRegionIds.stream()
                .filter(Objects::nonNull).map(String::strip).filter(value -> !value.isBlank())
                .distinct().toList();
        if (sourceRegionIds.isEmpty()) throw new IllegalArgumentException("sourceRegionIds are required");
        engineId = engineId == null ? "" : engineId.strip();
        options = options == null ? Map.of() : Map.copyOf(options);
    }
}
