package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageMap;

import java.util.List;

public record PdfPageMapResolution(PdfPageMap pageMap, Source source, List<String> warnings) {
    public PdfPageMapResolution {
        if (pageMap == null || source == null) throw new IllegalArgumentException("PageMap resolution is required");
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public enum Source {
        SIDECAR,
        LAZY_REBUILT,
        V3_FALLBACK
    }
}
