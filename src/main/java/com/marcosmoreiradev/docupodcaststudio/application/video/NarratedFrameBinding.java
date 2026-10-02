package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentRectangle;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentPresentationMode;

import java.util.Objects;
import java.util.List;

/**
 * Frozen narration-to-visual decision consumed by preflight, diagnostics and rendering.
 * Identity is carried by sourceBlockId/regionId; readingOrder is diagnostic ordering only.
 */
public record NarratedFrameBinding(
        String frameId,
        String segmentId,
        String sourceBlockId,
        List<String> sourceBlockIds,
        String regionId,
        String sourceBlockType,
        int pageNumber,
        int readingOrder,
        DocumentPresentationMode presentationMode,
        VisualSource visualSource,
        DocumentContentRectangle sourceBBox,
        DocumentContentRectangle cropBBox,
        String imageRelativePath,
        String audioPath,
        long durationMillis,
        String renderedTextHash,
        String expectedSourceTextHash,
        int sourceTextStart,
        int sourceTextEnd,
        String sourceTextPreview,
        List<Integer> sourceLineIndices,
        List<Integer> sourceWordIndices,
        List<DocumentContentRectangle> expectedFragmentBboxes,
        boolean fragmentCovered,
        Quality quality,
        String reason
) {
    public NarratedFrameBinding {
        frameId = token(frameId, "frameId");
        segmentId = token(segmentId, "segmentId");
        sourceBlockId = token(sourceBlockId, "sourceBlockId");
        sourceBlockIds = sourceBlockIds == null ? List.of() : sourceBlockIds.stream()
                .filter(Objects::nonNull).map(String::strip)
                .filter(value -> !value.isBlank()).distinct().toList();
        regionId = token(regionId, "regionId");
        sourceBlockType = optional(sourceBlockType);
        pageNumber = Math.max(0, pageNumber);
        readingOrder = Math.max(-1, readingOrder);
        presentationMode = Objects.requireNonNull(presentationMode, "presentationMode");
        visualSource = Objects.requireNonNull(visualSource, "visualSource");
        imageRelativePath = portablePath(imageRelativePath);
        audioPath = portablePath(audioPath);
        durationMillis = Math.max(0L, durationMillis);
        renderedTextHash = optional(renderedTextHash);
        expectedSourceTextHash = optional(expectedSourceTextHash);
        sourceTextStart = Math.max(0, sourceTextStart);
        sourceTextEnd = Math.max(sourceTextStart, sourceTextEnd);
        sourceTextPreview = optional(sourceTextPreview);
        sourceLineIndices = normalizeIndices(sourceLineIndices);
        sourceWordIndices = normalizeIndices(sourceWordIndices);
        expectedFragmentBboxes = expectedFragmentBboxes == null ? List.of()
                : expectedFragmentBboxes.stream().filter(Objects::nonNull).toList();
        quality = Objects.requireNonNull(quality, "quality");
        reason = optional(reason);
    }

    public boolean wrongVisual() {
        return quality == Quality.WRONG_VISUAL;
    }

    public enum VisualSource {
        GENERATED_TEXT_FRAME,
        PDF_REGION_CROP,
        PAGE_FALLBACK,
        WORD_SOURCE_CAPTURE,
        USER_SELECTED_ASSET
    }

    public enum Quality {
        CORRECT,
        COARSE_BUT_VALID,
        WRONG_VISUAL
    }

    private static String token(String value, String field) {
        String normalized = optional(value);
        if (normalized.isBlank() || normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must be a non-blank token");
        }
        return normalized;
    }

    private static String optional(String value) {
        return value == null ? "" : value.strip();
    }

    private static String portablePath(String value) {
        String normalized = optional(value).replace('\\', '/');
        if (normalized.isBlank()) return "";
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:/.*")
                || normalized.startsWith("../") || normalized.contains("/../")
                || normalized.startsWith("./") || normalized.contains("://")
                || normalized.toLowerCase(java.util.Locale.ROOT).startsWith("file:")) {
            throw new IllegalArgumentException("Path must be project-relative: " + value);
        }
        return normalized;
    }

    private static List<Integer> normalizeIndices(List<Integer> values) {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull)
                .filter(value -> value >= 0).distinct().sorted().toList();
    }
}
