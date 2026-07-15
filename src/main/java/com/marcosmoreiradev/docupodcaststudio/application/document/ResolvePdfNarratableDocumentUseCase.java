package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentImportIssue;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentImportReport;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/** Ensures PDF sources expose persistent narratable blocks for Word-like audio and playback flows. */
public final class ResolvePdfNarratableDocumentUseCase {
    private final BuildPdfOcrTextLayerUseCase ocrTextLayer;
    private final PdfTextLayerBlockMapper blockMapper;
    private final Supplier<OperationalSettings.OcrSettings> ocrSettingsSupplier;

    public ResolvePdfNarratableDocumentUseCase(BuildPdfNativeTextLayerUseCase nativeTextLayer,
                                               BuildPdfOcrTextLayerUseCase ocrTextLayer,
                                               PdfTextLayerBlockMapper blockMapper) {
        this(nativeTextLayer, ocrTextLayer, blockMapper, OperationalSettings.OcrSettings::defaults);
    }

    public ResolvePdfNarratableDocumentUseCase(BuildPdfNativeTextLayerUseCase nativeTextLayer,
                                               BuildPdfOcrTextLayerUseCase ocrTextLayer,
                                               PdfTextLayerBlockMapper blockMapper,
                                               Supplier<OperationalSettings.OcrSettings> ocrSettingsSupplier) {
        this.ocrTextLayer = Objects.requireNonNull(ocrTextLayer, "ocrTextLayer");
        this.blockMapper = blockMapper == null ? new PdfTextLayerBlockMapper() : blockMapper;
        this.ocrSettingsSupplier = ocrSettingsSupplier == null ? OperationalSettings.OcrSettings::defaults : ocrSettingsSupplier;
    }

    public PdfNarratableDocumentResolution resolve(PdfNarratableDocumentRequest request) {
        ReadableDocument document = request == null ? null : request.document();
        if (document == null || document.format() != SourceDocumentFormat.PDF) {
            return new PdfNarratableDocumentResolution(document, false, List.of(), List.of());
        }
        int pageCount = sourcePageCount(document);
        if (pageCount <= 0) {
            return new PdfNarratableDocumentResolution(document, false, List.of(), List.of());
        }
        List<Integer> targetPages = targetPages(document, pageCount, request.forceOcr(), request.maxPages(), request.targetPages());
        if (targetPages.isEmpty()) {
            return new PdfNarratableDocumentResolution(document, false, List.of(), List.of());
        }
        ArrayList<DocumentBlock> ocrBlocks = new ArrayList<>();
        ArrayList<DocumentImportIssue> issues = new ArrayList<>();
        ArrayList<Integer> processedPages = new ArrayList<>();
        int nextIndex = document.narratableBlockCount() == 0 ? 1 : nextBlockIndex(document.blocks());
        OperationalSettings.OcrSettings ocrSettings = currentOcrSettings();
        for (int page : targetPages) {
            try {
                PdfOcrPageResult result = ocrTextLayer.recognize(new PdfOcrRequest(
                        document.sourcePath(),
                        page,
                        ocrSettings.dpi(),
                        PdfOcrRequest.DEFAULT_MAX_PIXEL_COUNT,
                        ocrSettings.languages(),
                        request.forceOcr(),
                        cacheDirectory(request.cacheDirectory(), ocrSettings.cacheEnabled())));
                List<DocumentBlock> pageBlocks = blockMapper.blocksFromOcrLayer(
                        result.textLayer(), pageCount, visualRenderable(document), nextIndex);
                if (!pageBlocks.isEmpty()) {
                    processedPages.add(page);
                    ocrBlocks.addAll(pageBlocks);
                    nextIndex += pageBlocks.size();
                } else if (!result.words().isEmpty() || !result.lines().isEmpty()) {
                    issues.add(DocumentImportIssue.warning("pdf-ocr-text-discarded",
                            "OCR p. " + page + ": detecto " + result.lines().size()
                                    + " linea(s) y " + result.words().size()
                                    + " palabra(s), pero el filtro no encontro prosa narrable."));
                } else {
                    issues.add(DocumentImportIssue.warning("pdf-ocr-empty",
                            "OCR p. " + page + ": no se detecto prosa OCR narrable en esta pagina."));
                }
                result.warnings().forEach(warning -> issues.add(DocumentImportIssue.warning(
                        "pdf-ocr-page-warning", "OCR p. " + page + ": " + warning)));
            } catch (PdfOcrException ex) {
                if (ex.code() == PdfOcrErrorCode.TESSERACT_NOT_FOUND) {
                    issues.add(DocumentImportIssue.warning("pdf-ocr-runtime-missing",
                            "OCR p. " + page + ": OCR no configurado. Importa una carpeta Tesseract portable o configura ocr.tesseractExecutable."));
                } else {
                    issues.add(DocumentImportIssue.warning("pdf-ocr-unavailable",
                            "OCR p. " + page + ": [" + ex.code() + "] " + ex.getMessage()));
                }
            }
        }
        if (ocrBlocks.isEmpty()) {
            ReadableDocument withWarnings = mergeIssues(document, issues);
            return new PdfNarratableDocumentResolution(withWarnings, !issues.isEmpty(), processedPages, issues);
        }
        issues.add(DocumentImportIssue.info("pdf-ocr-applied",
                "Texto OCR local detectado y convertido en fragmentos narrables persistentes."));
        Set<Integer> replacedPages = new LinkedHashSet<>(processedPages);
        ArrayList<DocumentBlock> next = new ArrayList<>();
        for (DocumentBlock block : document.blocks()) {
            if (shouldReplacePdfPageBlock(block, replacedPages)) {
                continue;
            }
            next.add(block);
        }
        next.addAll(ocrBlocks);
        ReadableDocument resolved = new ReadableDocument(
                document.title(),
                document.format(),
                document.sourcePath(),
                next,
                new DocumentImportReport(mergedIssues(document.importReport().issues(), issues)));
        return new PdfNarratableDocumentResolution(resolved, true, processedPages, issues);
    }

    private static List<Integer> targetPages(ReadableDocument document,
                                             int pageCount,
                                             boolean forceOcr,
                                             int maxPages,
                                             List<Integer> requestedPages) {
        ArrayList<Integer> pages = new ArrayList<>();
        Iterable<Integer> candidates = requestedPages == null || requestedPages.isEmpty()
                ? java.util.stream.IntStream.rangeClosed(1, pageCount).boxed().toList()
                : requestedPages;
        for (int page : candidates) {
            if (page < 1 || page > pageCount || pages.size() >= maxPages) {
                continue;
            }
            if (!forceOcr && pageHasOcrBlock(document, page)) {
                continue;
            }
            pages.add(page);
        }
        return List.copyOf(pages);
    }

    private static boolean pageHasOcrBlock(ReadableDocument document, int page) {
        return document.blocks().stream().anyMatch(block -> page == sourcePage(block)
                && Boolean.parseBoolean(block.metadata().getOrDefault("ocr", "false"))
                && "ocr-local".equalsIgnoreCase(block.metadata().getOrDefault("extractionMode", "")));
    }

    private static boolean shouldReplacePdfPageBlock(DocumentBlock block, Set<Integer> replacedPages) {
        if (block == null || replacedPages == null || !replacedPages.contains(sourcePage(block))) {
            return false;
        }
        return true;
    }

    private static ReadableDocument mergeIssues(ReadableDocument document, List<DocumentImportIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return document;
        }
        return new ReadableDocument(document.title(), document.format(), document.sourcePath(), document.blocks(),
                new DocumentImportReport(mergedIssues(document.importReport().issues(), issues)));
    }

    private static List<DocumentImportIssue> mergedIssues(List<DocumentImportIssue> current, List<DocumentImportIssue> additions) {
        ArrayList<DocumentImportIssue> merged = new ArrayList<>(current == null ? List.of() : current);
        for (DocumentImportIssue issue : additions == null ? List.<DocumentImportIssue>of() : additions) {
            boolean duplicate = merged.stream().anyMatch(existing -> existing.code().equals(issue.code())
                    && existing.message().equals(issue.message())
                    && existing.blockId().equals(issue.blockId()));
            if (!duplicate) {
                merged.add(issue);
            }
        }
        return List.copyOf(merged);
    }

    private static int nextBlockIndex(List<DocumentBlock> blocks) {
        int max = 0;
        for (DocumentBlock block : blocks == null ? List.<DocumentBlock>of() : blocks) {
            String id = block.id();
            if (id != null && id.matches("B\\d+")) {
                try {
                    max = Math.max(max, Integer.parseInt(id.substring(1)));
                } catch (NumberFormatException ignored) {
                    // Keep scanning.
                }
            }
        }
        return max + 1;
    }

    private static int sourcePageCount(ReadableDocument document) {
        int max = 0;
        for (DocumentBlock block : document.blocks()) {
            max = Math.max(max, parsePositiveInt(block.metadata().get("sourcePageCount")));
            max = Math.max(max, sourcePage(block));
        }
        return max;
    }

    private static int sourcePage(DocumentBlock block) {
        return block == null ? 0 : parsePositiveInt(block.metadata().get("sourcePage"));
    }

    private static int parsePositiveInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            int value = Integer.parseInt(raw.strip());
            return Math.max(0, value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static boolean visualRenderable(ReadableDocument document) {
        return document.blocks().stream().anyMatch(block ->
                Boolean.parseBoolean(block.metadata().getOrDefault("visualRenderAvailable", "false")));
    }

    private OperationalSettings.OcrSettings currentOcrSettings() {
        try {
            OperationalSettings.OcrSettings settings = ocrSettingsSupplier.get();
            return settings == null ? OperationalSettings.OcrSettings.defaults() : settings;
        } catch (RuntimeException ex) {
            return OperationalSettings.OcrSettings.defaults();
        }
    }

    private static Path cacheDirectory(Path configured, boolean cacheEnabled) {
        if (!cacheEnabled) {
            return null;
        }
        return configured == null
                ? Path.of(System.getProperty("java.io.tmpdir"), "docupodcast-studio", "pdf-ocr")
                : configured;
    }
}
