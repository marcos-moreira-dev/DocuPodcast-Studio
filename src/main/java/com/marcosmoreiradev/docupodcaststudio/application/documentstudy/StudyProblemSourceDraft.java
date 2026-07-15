package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Temporary source selected by the user before a technical problem is persisted. */
public record StudyProblemSourceDraft(
        String sourceId,
        String selectedText,
        String sourcePage,
        String bbox,
        Path sourceCropPath,
        String embeddedImageBase64,
        boolean visualRegion
) {
    private static final String EXTERNAL_IMAGE_PREFIX = "EXTIMG-";

    public StudyProblemSourceDraft {
        sourceId = requireToken(sourceId, "sourceId");
        selectedText = normalize(selectedText);
        sourcePage = normalize(sourcePage);
        bbox = normalize(bbox);
        sourceCropPath = sourceCropPath == null ? null : sourceCropPath.toAbsolutePath().normalize();
        embeddedImageBase64 = normalize(embeddedImageBase64);
    }

    public static StudyProblemSourceDraft fromBlock(DocumentBlock block, Path sourceCropPath) {
        Objects.requireNonNull(block, "block");
        return new StudyProblemSourceDraft(
                block.id(),
                block.text(),
                block.metadata().getOrDefault("sourcePage", ""),
                block.metadata().getOrDefault("bbox", ""),
                sourceCropPath,
                block.metadata().getOrDefault("embeddedImageBase64", ""),
                false);
    }

    public static StudyProblemSourceDraft visualRegion(String sourceId, String selectedText, String sourcePage,
                                                       String bbox, Path sourceCropPath) {
        return new StudyProblemSourceDraft(sourceId, selectedText, sourcePage, bbox, sourceCropPath, "", true);
    }

    public static StudyProblemSourceDraft externalImage(String sourceId, Path imagePath) {
        String id = sourceId == null || sourceId.isBlank()
                ? EXTERNAL_IMAGE_PREFIX + java.util.UUID.randomUUID()
                : sourceId;
        if (!id.startsWith(EXTERNAL_IMAGE_PREFIX)) {
            id = EXTERNAL_IMAGE_PREFIX + id;
        }
        return new StudyProblemSourceDraft(id, "Imagen externa del problema.", "", "", imagePath, "", true);
    }

    public boolean hasText() {
        return !selectedText.isBlank();
    }

    public boolean hasCrop() {
        return sourceCropPath != null && Files.isRegularFile(sourceCropPath);
    }

    public boolean externalImage() {
        return sourceId.startsWith(EXTERNAL_IMAGE_PREFIX);
    }

    public boolean hasUsableContent() {
        return hasText() || hasCrop() || !embeddedImageBase64.isBlank();
    }

    private static String requireToken(String value, String field) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
