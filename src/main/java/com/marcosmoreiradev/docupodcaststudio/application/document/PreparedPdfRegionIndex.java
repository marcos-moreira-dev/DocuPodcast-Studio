package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;

import java.util.ArrayList;
import java.util.List;

/** Volatile lookup index rebuilt from canonical page JSON and never persisted. */
public record PreparedPdfRegionIndex(List<Entry> regions, List<Entry> headings) {
    public PreparedPdfRegionIndex {
        regions = regions == null ? List.of() : List.copyOf(regions);
        headings = headings == null ? List.of() : List.copyOf(headings);
    }

    public static PreparedPdfRegionIndex from(List<PreparedPdfPage> pages) {
        ArrayList<Entry> all = new ArrayList<>();
        ArrayList<Entry> structural = new ArrayList<>();
        if (pages != null) {
            pages.stream().sorted(java.util.Comparator.comparingInt(PreparedPdfPage::pageNumber))
                    .forEach(page -> page.regions().stream()
                            .sorted(java.util.Comparator.comparingInt(PdfRegion::effectiveReadingOrder))
                            .forEach(region -> {
                                Entry entry = new Entry(page.pageNumber(), page.widthPoints(),
                                        page.heightPoints(), region);
                                all.add(entry);
                                if (region.effectiveType() == PdfRegionType.TITLE
                                        || region.effectiveType() == PdfRegionType.HEADING
                                        || region.effectiveType() == PdfRegionType.SUBHEADING) {
                                    structural.add(entry);
                                }
                            }));
        }
        return new PreparedPdfRegionIndex(all, structural);
    }

    public record Entry(int pageNumber, double pageWidthPoints, double pageHeightPoints,
                        PdfRegion region) {
        public Entry {
            if (pageNumber <= 0) throw new IllegalArgumentException("pageNumber must be positive");
            if (pageWidthPoints <= 0 || pageHeightPoints <= 0) {
                throw new IllegalArgumentException("page dimensions must be positive");
            }
            region = java.util.Objects.requireNonNull(region, "region");
        }
    }
}
