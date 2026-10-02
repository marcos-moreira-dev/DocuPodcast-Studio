package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** One ordered unit shared by SideDock, audio coverage and documentary video. */
public record DocumentContentItem(
        String contentId,
        DocumentContentKind kind,
        String title,
        String narrationText,
        List<String> narrationSegmentIds,
        List<String> sourceIds,
        List<String> legacyContentIds,
        String fingerprint,
        long revision,
        DocumentPresentationMode presentationMode,
        DocumentContentAnchor anchor
) {
    public DocumentContentItem {
        contentId = token(contentId, "contentId");
        kind = Objects.requireNonNullElse(kind, DocumentContentKind.PROSE);
        title = title == null ? "" : title.strip();
        narrationText = narrationText == null ? "" : narrationText.strip();
        narrationSegmentIds = normalizedIds(narrationSegmentIds);
        sourceIds = normalizedIds(sourceIds);
        String canonicalContentId = contentId;
        legacyContentIds = normalizedIds(legacyContentIds).stream()
                .filter(value -> !value.equals(canonicalContentId)).toList();
        fingerprint = token(fingerprint, "fingerprint");
        revision = Math.max(1L, revision);
        presentationMode = Objects.requireNonNullElse(presentationMode,
                kind.secondarySemanticComponent()
                        ? DocumentPresentationMode.SOURCE_CAPTURE
                        : DocumentPresentationMode.TEXT_RENDER);
        anchor = Objects.requireNonNull(anchor, "anchor");
    }

    public DocumentContentItem(
            String contentId,
            DocumentContentKind kind,
            String title,
            String narrationText,
            List<String> narrationSegmentIds,
            List<String> sourceIds,
            String fingerprint,
            long revision,
            DocumentContentAnchor anchor) {
        this(contentId, kind, title, narrationText, narrationSegmentIds,
                sourceIds, List.of(), fingerprint, revision,
                kind != null && kind.secondarySemanticComponent()
                        ? DocumentPresentationMode.SOURCE_CAPTURE
                        : DocumentPresentationMode.TEXT_RENDER,
                anchor);
    }

    public DocumentContentItem(
            String contentId,
            DocumentContentKind kind,
            String title,
            String narrationText,
            List<String> narrationSegmentIds,
            List<String> sourceIds,
            List<String> legacyContentIds,
            String fingerprint,
            long revision,
            DocumentContentAnchor anchor) {
        this(contentId, kind, title, narrationText, narrationSegmentIds,
                sourceIds, legacyContentIds, fingerprint, revision,
                kind != null && kind.secondarySemanticComponent()
                        ? DocumentPresentationMode.SOURCE_CAPTURE
                        : DocumentPresentationMode.TEXT_RENDER,
                anchor);
    }

    public boolean matchesContentId(String candidate) {
        String normalized = candidate == null ? "" : candidate.strip();
        return contentId.equals(normalized) || legacyContentIds.contains(normalized);
    }

    public boolean narratable() {
        return !narrationText.isBlank() && !narrationSegmentIds.isEmpty();
    }

    public boolean secondarySemanticComponent() {
        return kind.secondarySemanticComponent();
    }

    public Optional<PdfContentAnchor> pdfAnchor() {
        return anchor instanceof PdfContentAnchor pdf ? Optional.of(pdf) : Optional.empty();
    }

    public Optional<WordContentAnchor> wordAnchor() {
        return anchor instanceof WordContentAnchor word ? Optional.of(word) : Optional.empty();
    }

    private static List<String> normalizedIds(List<String> values) {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull)
                .map(String::strip).filter(value -> !value.isBlank()).distinct().toList();
    }

    private static String token(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank() || normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must be a non-blank token");
        }
        return normalized;
    }
}
