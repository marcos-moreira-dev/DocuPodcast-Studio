package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;

/** One or more exact PDF boxes highlighted for the active narration cue. */
public record PdfVisualTextHighlight(
        int pageNumber,
        List<PdfPageRegion> regions,
        String text,
        PdfTextLayerOrigin origin
) {
    public PdfVisualTextHighlight {
        regions = regions == null ? List.of() : regions.stream()
                .filter(java.util.Objects::nonNull).toList();
        if (pageNumber <= 0) {
            pageNumber = regions.isEmpty() ? 0 : regions.getFirst().pageNumber();
        }
        text = text == null ? "" : text.strip();
        origin = origin == null ? PdfTextLayerOrigin.UNAVAILABLE : origin;
    }

    public PdfVisualTextHighlight(
            int pageNumber, PdfPageRegion region, String text,
            PdfTextLayerOrigin origin) {
        this(pageNumber, region == null ? List.of() : List.of(region), text, origin);
    }

    /** Compatibility accessor and scroll anchor; rendering uses all regions(). */
    public PdfPageRegion region() {
        return regions.isEmpty() ? null : regions.getFirst();
    }

    public boolean available() {
        return pageNumber > 0 && !regions.isEmpty()
                && regions.stream().allMatch(region -> region.pageNumber() == pageNumber)
                && origin != PdfTextLayerOrigin.UNAVAILABLE;
    }

    /**
     * One calm playback surface. Fine line boxes remain available in
     * {@link #regions()} for diagnostics and hit testing, but ordinary playback
     * paints only their canonical union.
     */
    public PdfPageRegion visibleRegion() {
        if (regions.isEmpty()) return null;
        PdfPageRegion first = regions.getFirst();
        return new PdfPageRegion(pageNumber,
                regions.stream().mapToDouble(PdfPageRegion::xMinPoints).min().orElse(first.xMinPoints()),
                regions.stream().mapToDouble(PdfPageRegion::yMinPoints).min().orElse(first.yMinPoints()),
                regions.stream().mapToDouble(PdfPageRegion::xMaxPoints).max().orElse(first.xMaxPoints()),
                regions.stream().mapToDouble(PdfPageRegion::yMaxPoints).max().orElse(first.yMaxPoints()),
                first.pageWidthPoints(), first.pageHeightPoints());
    }
}
