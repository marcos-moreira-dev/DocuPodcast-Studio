package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;

import java.nio.file.Path;
import java.util.List;

/** Request to resolve PDF text layers from native bbox and optional local OCR. */
public record PdfResolvedTextLayerRequest(
        ReadableDocument document,
        List<Integer> targetPages,
        PdfTextResolutionPolicy policy,
        Path cacheDirectory,
        int dpi,
        String languages
) {
    public PdfResolvedTextLayerRequest {
        targetPages = targetPages == null ? List.of() : targetPages.stream()
                .filter(page -> page != null && page > 0)
                .distinct()
                .sorted()
                .toList();
        policy = policy == null ? PdfTextResolutionPolicy.NATIVE_ONLY : policy;
        dpi = dpi <= 0 ? PdfOcrRequest.DEFAULT_DPI : dpi;
        languages = languages == null || languages.isBlank() ? PdfOcrRequest.DEFAULT_LANGUAGES : languages.strip();
    }
}
