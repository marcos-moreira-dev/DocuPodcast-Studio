package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Enriches weak PDF outlines with native/OCR text from early pages without mutating the source. */
public final class BuildPdfEnhancedOutlineUseCase {
    private static final int OCR_CONTENTS_PAGE_LIMIT = 30;

    private final BuildDocumentOutlineUseCase baseOutline;
    private final BuildPdfResolvedTextLayerUseCase resolvedTextLayer;

    public BuildPdfEnhancedOutlineUseCase(BuildDocumentOutlineUseCase baseOutline,
                                          BuildPdfResolvedTextLayerUseCase resolvedTextLayer) {
        this.baseOutline = baseOutline == null ? new BuildDocumentOutlineUseCase() : baseOutline;
        this.resolvedTextLayer = resolvedTextLayer;
    }

    public DocumentOutlineProjection build(ReadableDocument document, Path cacheDirectory) {
        DocumentOutlineProjection base = baseOutline.build(document);
        if (document == null || document.format() != SourceDocumentFormat.PDF || strongPdfOutline(base)
                || resolvedTextLayer == null) {
            return base;
        }
        int pageCount = sourcePageCount(document);
        if (pageCount <= 0) {
            return base;
        }
        List<Integer> targetPages = new ArrayList<>();
        for (int page = 1; page <= Math.min(OCR_CONTENTS_PAGE_LIMIT, pageCount); page++) {
            targetPages.add(page);
        }
        PdfResolvedTextLayerProjection resolved = resolvedTextLayer.resolve(new PdfResolvedTextLayerRequest(
                document,
                targetPages,
                PdfTextResolutionPolicy.OCR_WHEN_UNAVAILABLE,
                cacheDirectory,
                PdfOcrRequest.DEFAULT_DPI,
                PdfOcrRequest.DEFAULT_LANGUAGES));
        ReadableDocument enriched = enrichedDocument(document, resolved.layers(), pageCount);
        DocumentOutlineProjection candidate = baseOutline.build(enriched);
        if (candidate.origin() == DocumentOutlineOrigin.CONTENTS
                && candidate.indexedEntryCount() >= 4
                && !containsSyntheticBlock(candidate.entries())) {
            return new DocumentOutlineProjection(candidate.origin(), candidate.title(),
                    candidate.detail() + " Incluye capa textual nativa/OCR local cuando fue necesaria.",
                    candidate.entries(), candidate.indexedEntryCount(), candidate.flatLimit());
        }
        return base;
    }

    private static boolean strongPdfOutline(DocumentOutlineProjection projection) {
        if (projection == null) {
            return false;
        }
        return projection.origin() == DocumentOutlineOrigin.PDF_BOOKMARKS
                || projection.origin() == DocumentOutlineOrigin.CONTENTS
                || projection.origin() == DocumentOutlineOrigin.HEADINGS;
    }

    private static ReadableDocument enrichedDocument(ReadableDocument document, List<PdfTextLayer> layers, int pageCount) {
        List<DocumentBlock> blocks = new ArrayList<>(document.blocks());
        Set<String> seenTextLayerLines = new LinkedHashSet<>();
        for (PdfTextLayer layer : layers) {
            if (!layer.available() || layer.pageNumber() > OCR_CONTENTS_PAGE_LIMIT) {
                continue;
            }
            int ordinal = 0;
            for (PdfTextLine line : layer.lines()) {
                String text = line.text().replaceAll("\\s+", " ").strip();
                if (text.isBlank() || !seenTextLayerLines.add(text.toLowerCase(java.util.Locale.ROOT))) {
                    continue;
                }
                blocks.add(DocumentBlock.of(
                        "pdf-text-layer-" + layer.pageNumber() + "-" + ordinal++,
                        DocumentBlockType.IGNORED,
                        text,
                        "",
                        Map.of(
                                "sourcePage", Integer.toString(layer.pageNumber()),
                                "sourcePageCount", Integer.toString(pageCount),
                                "textLayerOrigin", layer.origin().name())));
            }
        }
        return new ReadableDocument(document.title(), document.format(), document.sourcePath(), blocks);
    }

    private static int sourcePageCount(ReadableDocument document) {
        int max = 0;
        for (DocumentBlock block : document.blocks()) {
            max = Math.max(max, parsePositiveInt(block.metadata().get("sourcePageCount")));
            max = Math.max(max, parsePositiveInt(block.metadata().get("sourcePage")));
        }
        return max;
    }

    private static int parsePositiveInt(String value) {
        if (value == null || value.isBlank() || !value.strip().matches("\\d+")) {
            return 0;
        }
        int parsed = Integer.parseInt(value.strip());
        return Math.max(0, parsed);
    }

    private static boolean containsSyntheticBlock(List<DocumentOutlineEntry> entries) {
        for (DocumentOutlineEntry entry : entries == null ? List.<DocumentOutlineEntry>of() : entries) {
            if (entry.blockId().startsWith("pdf-text-layer-") || containsSyntheticBlock(entry.children())) {
                return true;
            }
        }
        return false;
    }
}
