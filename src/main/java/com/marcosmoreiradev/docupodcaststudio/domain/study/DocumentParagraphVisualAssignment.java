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
        DocumentMascotPosition mascotPosition
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
        if (activeSource == DocumentVisualSource.IMPORTED && importedImageAssetId.isBlank()) {
            activeSource = drawnImageAssetId.isBlank() ? DocumentVisualSource.NONE : DocumentVisualSource.DRAWN;
        }
        if (activeSource == DocumentVisualSource.DRAWN && drawnImageAssetId.isBlank()) {
            activeSource = importedImageAssetId.isBlank() ? DocumentVisualSource.NONE : DocumentVisualSource.IMPORTED;
        }
    }

    public static DocumentParagraphVisualAssignment empty(String blockId) {
        return new DocumentParagraphVisualAssignment(blockId, "", "", "", "",
                DocumentVisualSource.NONE, "", DocumentMascotPosition.BOTTOM_RIGHT);
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
                mascotAssetId, mascotPosition);
    }

    public DocumentParagraphVisualAssignment withDrawnImage(String assetId, String stateRelativePath) {
        return new DocumentParagraphVisualAssignment(blockId, sourceTextFingerprint, importedImageAssetId,
                assetId, stateRelativePath, DocumentVisualSource.DRAWN, mascotAssetId, mascotPosition);
    }

    public DocumentParagraphVisualAssignment withActiveSource(DocumentVisualSource source) {
        return new DocumentParagraphVisualAssignment(blockId, sourceTextFingerprint, importedImageAssetId,
                drawnImageAssetId, drawnStateRelativePath, source, mascotAssetId, mascotPosition);
    }

    public DocumentParagraphVisualAssignment withMascot(String assetId, DocumentMascotPosition position) {
        return new DocumentParagraphVisualAssignment(blockId, sourceTextFingerprint, importedImageAssetId,
                drawnImageAssetId, drawnStateRelativePath, activeSource, assetId, position);
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
