package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

import java.util.List;
import java.util.Objects;

/** Persisted page-level decisions and engines; region evidence remains on each region. */
public record PdfPageAnalysisProfile(
        PdfPageRole automaticRole,
        PdfPageRole roleOverride,
        PdfDocumentLanguageProfile language,
        PdfPreparationProfile preparationProfile,
        int normalizedRotationDegrees,
        List<String> analysisEngineIds
) {
    public PdfPageAnalysisProfile {
        automaticRole = Objects.requireNonNullElse(automaticRole, PdfPageRole.UNKNOWN);
        language = Objects.requireNonNullElseGet(language, PdfDocumentLanguageProfile::undetermined);
        preparationProfile = Objects.requireNonNullElse(preparationProfile, PdfPreparationProfile.STANDARD);
        normalizedRotationDegrees = normalizeRotation(normalizedRotationDegrees);
        analysisEngineIds = analysisEngineIds == null ? List.of() : analysisEngineIds.stream()
                .filter(Objects::nonNull).map(String::strip).filter(value -> !value.isBlank())
                .distinct().toList();
    }

    public static PdfPageAnalysisProfile defaults() {
        return new PdfPageAnalysisProfile(PdfPageRole.UNKNOWN, null,
                PdfDocumentLanguageProfile.undetermined(), PdfPreparationProfile.STANDARD, 0, List.of());
    }

    public PdfPageRole effectiveRole() {
        return roleOverride == null ? automaticRole : roleOverride;
    }

    private static int normalizeRotation(int value) {
        int normalized = Math.floorMod(value, 360);
        if (normalized % 90 != 0) {
            throw new IllegalArgumentException("PDF rotation must be a multiple of 90");
        }
        return normalized;
    }
}
