package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfNativePageExtractor;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRegion;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextLayer;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextLayerOrigin;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextLine;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextToken;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRequest;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessResult;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.process.DefaultExternalProcessRunner;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Local Poppler bbox extractor used before deciding whether OCR is necessary. */
public final class PopplerPdfNativePageExtractor implements PdfNativePageExtractor {
    @Override
    public PdfTextLayer extract(Path sourcePdf, int pageNumber) {
        if (sourcePdf == null || pageNumber <= 0) return unavailable(pageNumber, "Solicitud nativa inválida.");
        Path bboxOutput = null;
        try {
            /*
             * bbox XHTML can exceed the bounded diagnostic capture used by the
             * common process runner. Ask Poppler to write a temporary file so
             * the XML prologue is never discarded and the user's PDF remains
             * read-only.
             */
            bboxOutput = Files.createTempFile("docupodcast-pdf-bbox-", ".xhtml");
            ExternalProcessResult result = new DefaultExternalProcessRunner().run(
                    ExternalProcessRequest.of(List.of(
                                    "pdftotext", "-f", Integer.toString(pageNumber),
                                    "-l", Integer.toString(pageNumber),
                                    "-bbox-layout", sourcePdf.toString(), bboxOutput.toString()),
                            "pdf-native-page-" + pageNumber, Duration.ofSeconds(20)));
            if (!result.succeeded() || !Files.isRegularFile(bboxOutput)) {
                return unavailable(pageNumber, "pdftotext no produjo una capa bbox válida.");
            }
            String xhtml = Files.readString(bboxOutput, StandardCharsets.UTF_8)
                    .replace("\uFEFF", "");
            PdfBboxExtraction parsed = new PdfBboxLayoutParser().parse(xhtml);
            ArrayList<PdfTextLine> lines = new ArrayList<>();
            for (PdfBboxTextBlock block : parsed.pages().stream()
                    .flatMap(page -> page.blocks().stream()).toList()) {
                int actualPage = pageNumber;
                PdfPageRegion region = new PdfPageRegion(actualPage,
                        block.bbox().xMin(), block.bbox().yMin(),
                        block.bbox().xMax(), block.bbox().yMax(),
                        block.pageWidth(), block.pageHeight());
                PdfTextToken token = new PdfTextToken(block.text(), region, 0.94);
                lines.add(new PdfTextLine(actualPage, block.text(), region, List.of(token), 0.94));
            }
            if (lines.isEmpty()) return unavailable(pageNumber, "La página no contiene texto nativo bbox.");
            return new PdfTextLayer(pageNumber, PdfTextLayerOrigin.NATIVE_BBOX, lines, List.of());
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            return unavailable(pageNumber, "Extracción nativa no disponible: " + ex.getMessage());
        } finally {
            if (bboxOutput != null) {
                try {
                    Files.deleteIfExists(bboxOutput);
                } catch (Exception ignored) {
                    // Best effort cleanup; canonical project data is unaffected.
                }
            }
        }
    }

    private static PdfTextLayer unavailable(int page, String warning) {
        return new PdfTextLayer(Math.max(1, page), PdfTextLayerOrigin.UNAVAILABLE,
                List.of(), List.of(warning));
    }
}
