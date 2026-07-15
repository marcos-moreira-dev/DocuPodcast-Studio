package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfNarratableDocumentRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfNarratableDocumentResolution;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.presentation.process.FxBackgroundTaskRunner;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.concurrent.Task;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/** Keeps PDF text/OCR preparation close to the visible page without blocking JavaFX. */
final class PdfVisibleTextPreparationCoordinator {
    private static final int MAX_VISIBLE_WINDOW_PAGES = 4;

    private final DocuPodcastShellViewModel viewModel;
    private final Supplier<Path> cacheDirectorySupplier;
    private final FxBackgroundTaskRunner backgroundTaskRunner = new FxBackgroundTaskRunner();
    private final LinkedHashSet<Integer> queue = new LinkedHashSet<>();
    private final Set<PageKey> autoAttemptedPages = new HashSet<>();
    private boolean running;
    private Path queuedSourcePath;

    PdfVisibleTextPreparationCoordinator(DocuPodcastShellViewModel viewModel, Supplier<Path> cacheDirectorySupplier) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
        this.cacheDirectorySupplier = Objects.requireNonNull(cacheDirectorySupplier, "cacheDirectorySupplier");
    }

    void prepareVisibleWindow(ReadableDocument document, int visiblePage) {
        // OCR is intentionally click-driven; opening or scrolling a PDF must not enqueue analysis.
    }

    void preparePageNow(ReadableDocument document, int page) {
        enqueue(document, List.of(page), true);
    }

    private void enqueue(ReadableDocument document, List<Integer> pages, boolean priority) {
        if (!shouldUsePdf(document) || pages.isEmpty()) {
            return;
        }
        resetIfSourceChanged(document.sourcePath());
        LinkedHashSet<Integer> nextPages = new LinkedHashSet<>();
        for (Integer page : pages) {
            int safePage = page == null ? 0 : page;
            PageKey key = new PageKey(document.sourcePath(), safePage);
            if (safePage > 0 && needsPage(document, safePage) && (priority || !autoAttemptedPages.contains(key))) {
                nextPages.add(safePage);
                if (priority) {
                    autoAttemptedPages.remove(key);
                }
            }
        }
        if (nextPages.isEmpty()) {
            return;
        }
        if (priority) {
            LinkedHashSet<Integer> reordered = new LinkedHashSet<>(nextPages);
            reordered.addAll(queue);
            queue.clear();
            queue.addAll(reordered);
            viewModel.updateStatusMessage("Analizando pagina PDF " + nextPages.iterator().next() + " por clic...");
        } else {
            queue.addAll(nextPages);
        }
        startNext();
    }

    private void startNext() {
        if (running) {
            return;
        }
        ReadableDocument document = viewModel.currentDocumentProperty().get();
        if (!shouldUsePdf(document)) {
            queue.clear();
            return;
        }
        resetIfSourceChanged(document.sourcePath());
        Integer page = pollNextNeededPage(document);
        if (page == null) {
            return;
        }
        running = true;
        autoAttemptedPages.add(new PageKey(document.sourcePath(), page));
        viewModel.updateStatusMessage("Analizando pagina PDF " + page + " por clic...");
        Task<PdfNarratableDocumentResolution> task = new Task<>() {
            @Override
            protected PdfNarratableDocumentResolution call() {
                return viewModel.applicationServices().document().resolvePdfNarratableDocument()
                        .resolve(new PdfNarratableDocumentRequest(document, cacheDirectorySupplier.get(), false, 1, List.of(page)));
            }
        };
        task.setOnSucceeded(event -> complete(page, task.getValue()));
        task.setOnFailed(event -> fail(page, task.getException()));
        backgroundTaskRunner.start("docupodcast-pdf-visible-text-" + page, task);
    }

    private Integer pollNextNeededPage(ReadableDocument document) {
        while (!queue.isEmpty()) {
            Integer page = queue.iterator().next();
            queue.remove(page);
            if (page != null && needsPage(document, page)) {
                return page;
            }
        }
        return null;
    }

    private void complete(int page, PdfNarratableDocumentResolution resolution) {
        running = false;
        ReadableDocument current = viewModel.currentDocumentProperty().get();
        if (resolution == null || resolution.document() == null || current == null
                || !Objects.equals(current.sourcePath(), resolution.document().sourcePath())) {
            startNext();
            return;
        }
        if (resolution != null && resolution.changed() && resolution.document() != null && !resolution.pagesProcessed().isEmpty()) {
            int fragmentCount = countReadyOcrBlocks(resolution.document(), page);
            viewModel.replaceCurrentDocumentFromApplication(resolution.document(),
                    "Pagina PDF " + page + " analizada: " + fragmentCount + " fragmentos OCR listos.");
        } else if (resolution != null && !resolution.issues().isEmpty()) {
            viewModel.updateStatusMessage(statusForIssue(page, resolution.issues().getFirst().code(),
                    resolution.issues().getFirst().message()));
        } else {
            viewModel.updateStatusMessage("Pagina PDF " + page + " ya tiene mapa textual.");
        }
        startNext();
    }

    private void fail(int page, Throwable error) {
        running = false;
        String message = error == null || error.getMessage() == null || error.getMessage().isBlank()
                ? "error desconocido"
                : error.getMessage();
        viewModel.updateStatusMessage("No se pudo analizar pagina PDF " + page + ": " + message);
        startNext();
    }

    private void resetIfSourceChanged(Path sourcePath) {
        if (Objects.equals(queuedSourcePath, sourcePath)) {
            return;
        }
        queuedSourcePath = sourcePath;
        queue.clear();
        autoAttemptedPages.clear();
        running = false;
    }

    private static List<Integer> visibleWindow(ReadableDocument document, int visiblePage) {
        int pageCount = pageCount(document);
        if (visiblePage <= 0 || pageCount <= 0) {
            return List.of();
        }
        ArrayList<Integer> pages = new ArrayList<>();
        int[] offsets = {0, 1, -1, 2};
        for (int offset : offsets) {
            int page = visiblePage + offset;
            if (page >= 1 && page <= pageCount) {
                pages.add(page);
            }
        }
        return pages.stream().distinct().limit(MAX_VISIBLE_WINDOW_PAGES).toList();
    }

    static boolean needsPage(ReadableDocument document, int page) {
        if (!shouldUsePdf(document) || page <= 0 || page > pageCount(document)) {
            return false;
        }
        for (DocumentBlock block : document.blocks()) {
            if (sourcePage(block) == page
                    && Boolean.parseBoolean(block.metadata().getOrDefault("ocr", "false"))
                    && "ocr-local".equalsIgnoreCase(block.metadata().getOrDefault("extractionMode", ""))
                    && block.narratable()
                    && "pdf-points".equalsIgnoreCase(block.metadata().getOrDefault("bboxUnits", ""))
                    && !block.metadata().getOrDefault("bbox", "").isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String statusForIssue(int page, String code, String message) {
        if ("pdf-ocr-runtime-missing".equals(code)) {
            return "OCR no configurado para pagina PDF " + page
                    + ". Usa Configurar OCR, importa una carpeta Tesseract portable o configura ocr.tesseractExecutable.";
        }
        if ("pdf-ocr-text-discarded".equals(code)) {
            return "Pagina PDF " + page + " analizada: OCR detecto texto, pero no prosa narrable. "
                    + (message == null ? "" : message);
        }
        if ("pdf-ocr-empty".equals(code)) {
            return "Pagina PDF " + page + " analizada: no se detecto prosa OCR narrable.";
        }
        return "Pagina PDF " + page + ": " + (message == null || message.isBlank() ? "OCR no disponible." : message);
    }

    private static int countReadyOcrBlocks(ReadableDocument document, int page) {
        if (document == null) {
            return 0;
        }
        int count = 0;
        for (DocumentBlock block : document.blocks()) {
            if (sourcePage(block) == page
                    && Boolean.parseBoolean(block.metadata().getOrDefault("ocr", "false"))
                    && "ocr-local".equalsIgnoreCase(block.metadata().getOrDefault("extractionMode", ""))
                    && block.narratable()) {
                count++;
            }
        }
        return count;
    }

    private static boolean shouldUsePdf(ReadableDocument document) {
        return document != null && document.format() == SourceDocumentFormat.PDF;
    }

    private static int pageCount(ReadableDocument document) {
        int max = 0;
        if (document == null) {
            return 0;
        }
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
            return Math.max(0, Integer.parseInt(raw.strip()));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private record PageKey(Path sourcePath, int page) {
    }
}
