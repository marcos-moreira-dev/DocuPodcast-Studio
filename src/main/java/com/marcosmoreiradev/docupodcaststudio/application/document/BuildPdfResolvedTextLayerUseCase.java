package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Resolves PDF text by composing persisted OCR layers with optional per-page OCR. */
public final class BuildPdfResolvedTextLayerUseCase {
    private final BuildPdfNativeTextLayerUseCase nativeTextLayer;
    private final BuildPdfOcrTextLayerUseCase ocrTextLayer;

    public BuildPdfResolvedTextLayerUseCase(BuildPdfNativeTextLayerUseCase nativeTextLayer,
                                            BuildPdfOcrTextLayerUseCase ocrTextLayer) {
        this.nativeTextLayer = nativeTextLayer == null ? new BuildPdfNativeTextLayerUseCase() : nativeTextLayer;
        this.ocrTextLayer = Objects.requireNonNull(ocrTextLayer, "ocrTextLayer");
    }

    public PdfResolvedTextLayerProjection resolve(PdfResolvedTextLayerRequest request) {
        ReadableDocument document = request == null ? null : request.document();
        if (document == null || document.format() != SourceDocumentFormat.PDF) {
            return new PdfResolvedTextLayerProjection(List.of(), List.of(), List.of("No hay PDF para resolver texto."));
        }
        Map<Integer, PdfTextLayer> layers = new LinkedHashMap<>();
        for (PdfTextLayer layer : nativeTextLayer.build(ocrOnlyDocument(document))) {
            layers.put(layer.pageNumber(), layer);
        }
        List<String> warnings = new ArrayList<>();
        List<Integer> attempted = new ArrayList<>();
        if (request.policy() == PdfTextResolutionPolicy.NATIVE_ONLY || request.targetPages().isEmpty()) {
            return new PdfResolvedTextLayerProjection(sortedLayers(layers), attempted, warnings);
        }
        for (int page : request.targetPages()) {
            PdfTextLayer current = layers.get(page);
            boolean shouldOcr = request.policy() == PdfTextResolutionPolicy.FORCE_OCR
                    || current == null
                    || !current.available();
            if (!shouldOcr) {
                continue;
            }
            attempted.add(page);
            try {
                PdfOcrPageResult ocr = ocrTextLayer.recognize(new PdfOcrRequest(
                        document.sourcePath(),
                        page,
                        request.dpi(),
                        PdfOcrRequest.DEFAULT_MAX_PIXEL_COUNT,
                        request.languages(),
                        request.policy() == PdfTextResolutionPolicy.FORCE_OCR,
                        request.cacheDirectory()));
                layers.put(page, ocr.textLayer());
                warnings.addAll(ocr.warnings());
            } catch (PdfOcrException ex) {
                warnings.add("OCR p. " + page + ": [" + ex.code() + "] " + ex.getMessage());
                layers.putIfAbsent(page, new PdfTextLayer(page, PdfTextLayerOrigin.UNAVAILABLE, List.of(),
                        List.of("OCR no disponible: " + ex.getMessage())));
            }
        }
        return new PdfResolvedTextLayerProjection(sortedLayers(layers), attempted, warnings);
    }

    private static List<PdfTextLayer> sortedLayers(Map<Integer, PdfTextLayer> layers) {
        return layers.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();
    }

    private static ReadableDocument ocrOnlyDocument(ReadableDocument document) {
        return document.withBlocks(document.blocks().stream()
                .filter(block -> isOcrBlock(block) || isVisualFallbackBlock(block) || !block.metadata().containsKey("sourcePage"))
                .toList());
    }

    private static boolean isOcrBlock(com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock block) {
        return block != null
                && "true".equalsIgnoreCase(block.metadata().getOrDefault("ocr", "false"))
                && "ocr-local".equalsIgnoreCase(block.metadata().getOrDefault("extractionMode", ""));
    }

    private static boolean isVisualFallbackBlock(com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock block) {
        return block != null
                && ("visual-fallback".equalsIgnoreCase(block.metadata().getOrDefault("extractionMode", ""))
                || "true".equalsIgnoreCase(block.metadata().getOrDefault("visualBlock", "false")));
    }
}
