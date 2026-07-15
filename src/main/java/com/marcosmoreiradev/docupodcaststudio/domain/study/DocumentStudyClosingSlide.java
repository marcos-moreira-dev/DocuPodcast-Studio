package com.marcosmoreiradev.docupodcaststudio.domain.study;

/** User-authored silent closing slide appended after the DOCX content. */
public record DocumentStudyClosingSlide(
        String id,
        String title,
        double durationSeconds,
        String imageAssetId
) {
    public DocumentStudyClosingSlide {
        id = token(id);
        title = title == null ? "" : title.strip();
        durationSeconds = DocumentTableSlideConfiguration.clamp(durationSeconds);
        imageAssetId = imageAssetId == null ? "" : imageAssetId.strip();
    }

    public static DocumentStudyClosingSlide empty(String id) {
        return new DocumentStudyClosingSlide(id, "", 6.0, "");
    }

    public DocumentStudyClosingSlide withTitle(String nextTitle) {
        return new DocumentStudyClosingSlide(id, nextTitle, durationSeconds, imageAssetId);
    }

    public DocumentStudyClosingSlide withDuration(double seconds) {
        return new DocumentStudyClosingSlide(id, title, seconds, imageAssetId);
    }

    public DocumentStudyClosingSlide withImage(String assetId) {
        return new DocumentStudyClosingSlide(id, title, durationSeconds, assetId);
    }

    private static String token(String value) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank() || normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("id is required and must not contain whitespace");
        }
        return normalized;
    }
}
