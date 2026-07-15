package com.marcosmoreiradev.docupodcaststudio.domain.study;

import java.util.Objects;

/** Source range captured from the read-only document for a study problem. */
public record StudySourceReference(
        String blockId,
        int startOffset,
        int endOffset,
        String selectedText,
        String sourceCropAssetId,
        String sourcePage,
        String bbox
) {
    public StudySourceReference {
        blockId = requireToken(blockId, "blockId");
        startOffset = Math.max(0, startOffset);
        endOffset = Math.max(startOffset, endOffset);
        selectedText = normalize(selectedText);
        sourceCropAssetId = normalize(sourceCropAssetId);
        sourcePage = normalize(sourcePage);
        bbox = normalize(bbox);
    }

    public static StudySourceReference fullBlock(String blockId, String text, String sourcePage, String bbox) {
        return fullBlock(blockId, text, sourcePage, bbox, "");
    }

    public static StudySourceReference fullBlock(String blockId, String text, String sourcePage, String bbox, String sourceCropAssetId) {
        String normalizedText = normalize(text);
        return new StudySourceReference(blockId, 0, normalizedText.length(), normalizedText, sourceCropAssetId, sourcePage, bbox);
    }

    public static StudySourceReference visualRegion(String sourceId, String selectedText, String sourcePage,
                                                    String bbox, String sourceCropAssetId) {
        String normalizedText = normalize(selectedText);
        return new StudySourceReference(sourceId, 0, normalizedText.length(), normalizedText,
                sourceCropAssetId, sourcePage, bbox);
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
        return Objects.toString(value, "").strip();
    }
}
