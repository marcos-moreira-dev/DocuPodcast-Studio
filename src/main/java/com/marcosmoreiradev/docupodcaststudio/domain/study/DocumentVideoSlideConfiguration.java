package com.marcosmoreiradev.docupodcaststudio.domain.study;

import java.util.Objects;

/** Format-neutral documentary slide configuration keyed by projection contentId. */
public record DocumentVideoSlideConfiguration(
        String contentId,
        String sourceFingerprint,
        DocumentParagraphVisualAssignment visual,
        double durationSeconds,
        boolean enabled,
        String sourceVisualAssetId,
        String sourceVisualFingerprint
) {
    public DocumentVideoSlideConfiguration {
        contentId = token(contentId, "contentId");
        sourceFingerprint = sourceFingerprint == null ? "" : sourceFingerprint.strip();
        visual = visual == null
                ? DocumentParagraphVisualAssignment.empty(contentId) : visual;
        if (!visual.blockId().equals(contentId)) {
            visual = new DocumentParagraphVisualAssignment(contentId,
                    visual.sourceTextFingerprint(), visual.importedImageAssetId(),
                    visual.drawnImageAssetId(), visual.drawnStateRelativePath(),
                    visual.activeSource(), visual.mascotAssetId(), visual.mascotPosition(),
                    visual.mascotSizePercent(), visual.subtitle(), visual.illustrationOnly());
        }
        durationSeconds = durationSeconds <= 0.0 ? 0.0
                : DocumentTableSlideConfiguration.clamp(durationSeconds);
        sourceVisualAssetId = sourceVisualAssetId == null ? "" : sourceVisualAssetId.strip();
        sourceVisualFingerprint = sourceVisualFingerprint == null
                ? "" : sourceVisualFingerprint.strip();
    }

    public static DocumentVideoSlideConfiguration empty(String contentId) {
        return new DocumentVideoSlideConfiguration(contentId, "",
                DocumentParagraphVisualAssignment.empty(contentId),
                0.0, true, "", "");
    }

    public DocumentVideoSlideConfiguration withVisual(
            DocumentParagraphVisualAssignment next) {
        return new DocumentVideoSlideConfiguration(contentId, sourceFingerprint,
                next, durationSeconds, enabled, sourceVisualAssetId,
                sourceVisualFingerprint);
    }

    public DocumentVideoSlideConfiguration withEnabled(boolean next) {
        return new DocumentVideoSlideConfiguration(contentId, sourceFingerprint,
                visual, durationSeconds, next, sourceVisualAssetId,
                sourceVisualFingerprint);
    }

    public DocumentVideoSlideConfiguration withDuration(double next) {
        return new DocumentVideoSlideConfiguration(contentId, sourceFingerprint,
                visual, next, enabled, sourceVisualAssetId, sourceVisualFingerprint);
    }

    public DocumentVideoSlideConfiguration withSourceVisualAsset(
            String assetId, String visualFingerprint, String contentFingerprint) {
        return new DocumentVideoSlideConfiguration(contentId, contentFingerprint,
                visual, durationSeconds, enabled, assetId, visualFingerprint);
    }

    private static String token(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank() || normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must be a non-blank token");
        }
        return normalized;
    }
}
