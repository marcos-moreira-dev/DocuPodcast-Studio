package com.marcosmoreiradev.docupodcaststudio.domain.study;

import java.util.Objects;

/** Project-side visual choices for one source DOCX paragraph. */
public record DocumentParagraphVisualAssignment(
        String blockId,
        String sourceTextFingerprint,
        String importedImageAssetId,
        String drawnImageAssetId,
        String drawnStateRelativePath,
        DocumentVisualSource activeSource,
        String mascotAssetId,
        DocumentMascotPosition mascotPosition,
        int mascotSizePercent,
        String subtitle,
        boolean illustrationOnly
) {
    public DocumentParagraphVisualAssignment {
        blockId = token(blockId, "blockId");
        sourceTextFingerprint = optional(sourceTextFingerprint);
        importedImageAssetId = optional(importedImageAssetId);
        drawnImageAssetId = optional(drawnImageAssetId);
        drawnStateRelativePath = portablePath(drawnStateRelativePath);
        activeSource = Objects.requireNonNullElse(activeSource, DocumentVisualSource.NONE);
        mascotAssetId = optional(mascotAssetId);
        mascotPosition = Objects.requireNonNullElse(mascotPosition, DocumentMascotPosition.BOTTOM_RIGHT);
        mascotSizePercent = Math.max(5, Math.min(40, mascotSizePercent));
        subtitle = optional(subtitle);
        if (activeSource == DocumentVisualSource.IMPORTED && importedImageAssetId.isBlank()) {
            activeSource = drawnImageAssetId.isBlank() ? DocumentVisualSource.NONE : DocumentVisualSource.DRAWN;
        }
        if (activeSource == DocumentVisualSource.DRAWN && drawnImageAssetId.isBlank()) {
            activeSource = importedImageAssetId.isBlank() ? DocumentVisualSource.NONE : DocumentVisualSource.IMPORTED;
        }
    }

    /** Backward-compatible constructor for assignments created before slide presentation options existed. */
    public DocumentParagraphVisualAssignment(
            String blockId,
            String sourceTextFingerprint,
            String importedImageAssetId,
            String drawnImageAssetId,
            String drawnStateRelativePath,
            DocumentVisualSource activeSource,
            String mascotAssetId,
            DocumentMascotPosition mascotPosition,
            String subtitle,
            boolean illustrationOnly
    ) {
        this(blockId, sourceTextFingerprint, importedImageAssetId, drawnImageAssetId,
                drawnStateRelativePath, activeSource, mascotAssetId, mascotPosition, 15,
                subtitle, illustrationOnly);
    }

    /** Backward-compatible constructor for assignments created before slide presentation options existed. */
    public DocumentParagraphVisualAssignment(
            String blockId,
            String sourceTextFingerprint,
            String importedImageAssetId,
            String drawnImageAssetId,
            String drawnStateRelativePath,
            DocumentVisualSource activeSource,
            String mascotAssetId,
            DocumentMascotPosition mascotPosition
    ) {
        this(blockId, sourceTextFingerprint, importedImageAssetId, drawnImageAssetId,
                drawnStateRelativePath, activeSource, mascotAssetId, mascotPosition, 15, "", false);
    }

    public static DocumentParagraphVisualAssignment empty(String blockId) {
        return new DocumentParagraphVisualAssignment(blockId, "", "", "", "",
                DocumentVisualSource.NONE, "", DocumentMascotPosition.BOTTOM_RIGHT, 15, "", false);
    }

    public String activeImageAssetId() {
        return switch (activeSource) {
            case IMPORTED -> importedImageAssetId;
            case DRAWN -> drawnImageAssetId;
            case NONE -> "";
        };
    }

    public DocumentParagraphVisualAssignment withImportedImage(String assetId) {
        return new DocumentParagraphVisualAssignment(blockId, sourceTextFingerprint, assetId,
                drawnImageAssetId, drawnStateRelativePath, DocumentVisualSource.IMPORTED,
                mascotAssetId, mascotPosition, mascotSizePercent, subtitle, illustrationOnly);
    }

    public DocumentParagraphVisualAssignment withDrawnImage(String assetId, String stateRelativePath) {
        return new DocumentParagraphVisualAssignment(blockId, sourceTextFingerprint, importedImageAssetId,
                assetId, stateRelativePath, DocumentVisualSource.DRAWN, mascotAssetId, mascotPosition,
                mascotSizePercent, subtitle, illustrationOnly);
    }

    public DocumentParagraphVisualAssignment withActiveSource(DocumentVisualSource source) {
        return new DocumentParagraphVisualAssignment(blockId, sourceTextFingerprint, importedImageAssetId,
                drawnImageAssetId, drawnStateRelativePath, source, mascotAssetId, mascotPosition,
                mascotSizePercent, subtitle, illustrationOnly);
    }

    public DocumentParagraphVisualAssignment withMascot(String assetId, DocumentMascotPosition position) {
        return new DocumentParagraphVisualAssignment(blockId, sourceTextFingerprint, importedImageAssetId,
                drawnImageAssetId, drawnStateRelativePath, activeSource, assetId, position,
                mascotSizePercent, subtitle, illustrationOnly);
    }

    public DocumentParagraphVisualAssignment withMascot(
            String assetId, DocumentMascotPosition position, int sizePercent) {
        return new DocumentParagraphVisualAssignment(blockId, sourceTextFingerprint, importedImageAssetId,
                drawnImageAssetId, drawnStateRelativePath, activeSource, assetId, position,
                sizePercent, subtitle, illustrationOnly);
    }

    public DocumentParagraphVisualAssignment withSubtitle(String value) {
        return new DocumentParagraphVisualAssignment(blockId, sourceTextFingerprint, importedImageAssetId,
                drawnImageAssetId, drawnStateRelativePath, activeSource, mascotAssetId, mascotPosition,
                mascotSizePercent, value, illustrationOnly);
    }

    public DocumentParagraphVisualAssignment withIllustrationOnly(boolean value) {
        return new DocumentParagraphVisualAssignment(blockId, sourceTextFingerprint, importedImageAssetId,
                drawnImageAssetId, drawnStateRelativePath, activeSource, mascotAssetId, mascotPosition,
                mascotSizePercent, subtitle, value);
    }

    private static String token(String value, String field) {
        String normalized = optional(value);
        if (normalized.isBlank() || normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " is required and must not contain whitespace");
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
                || normalized.startsWith("../") || normalized.contains("/../") || normalized.contains("://")) {
            throw new IllegalArgumentException("drawnStateRelativePath must be project-relative");
        }
        return normalized;
    }
}
