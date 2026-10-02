package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

/** Nullable fields represent independent user overrides over automatic PDF analysis. */
public record PdfRegionOverride(
        String text,
        PdfRegionType type,
        PdfNarratability narratability,
        Integer readingOrder
) {
    public PdfRegionOverride {
        text = text == null ? null : text.strip();
        if (readingOrder != null && readingOrder < 0) {
            throw new IllegalArgumentException("readingOrder override must be >= 0");
        }
    }

    public static PdfRegionOverride empty() {
        return new PdfRegionOverride(null, null, null, null);
    }

    public boolean emptyOverride() {
        return text == null && type == null && narratability == null && readingOrder == null;
    }
}
