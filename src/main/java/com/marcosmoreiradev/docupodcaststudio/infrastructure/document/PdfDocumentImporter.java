package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentImporter;
import com.marcosmoreiradev.docupodcaststudio.application.document.BuildPdfOcrTextLayerUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfDocumentInfo;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfNarratableTextClassifier;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOpenOptions;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOcrException;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOcrPageResult;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOcrRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRegion;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfRenderEngine;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfRenderErrorCode;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfRenderException;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextLayerBlockMapper;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextNormalizationReport;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextRepairService;
import com.marcosmoreiradev.docupodcaststudio.application.document.TesseractRuntimeLocator;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.process.DefaultExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRequest;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessResult;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimePathResolver;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentImportIssue;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentImportReport;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.zip.InflaterInputStream;

/**
 * Imports native-text PDFs and uses local OCR to make scanned/image-only PDFs narrable.
 */
public final class PdfDocumentImporter implements DocumentImporter {
    private static final int MIN_NATIVE_TEXT_CHARACTERS = 40;
    private static final int MIN_NATIVE_TEXT_WORDS = 8;
    private static final PdfTextRepairService TEXT_REPAIR = new PdfTextRepairService();
    private final PdfRenderEngine renderEngine;
    private final BuildPdfOcrTextLayerUseCase ocrTextLayer;
    private final PdfTextLayerBlockMapper ocrBlockMapper = new PdfTextLayerBlockMapper();

    public PdfDocumentImporter() {
        this(defaultRenderEngine());
    }

    PdfDocumentImporter(PdfRenderEngine renderEngine) {
        this(renderEngine, defaultOcrTextLayer(renderEngine));
    }

    public PdfDocumentImporter(PdfRenderEngine renderEngine, BuildPdfOcrTextLayerUseCase ocrTextLayer) {
        this.renderEngine = renderEngine == null ? new PdfBoxRenderEngine() : renderEngine;
        this.ocrTextLayer = ocrTextLayer;
    }

    @Override
    public boolean supports(Path sourceFile) {
        return PlainTextDocumentImporter.hasExtension(sourceFile, ".pdf");
    }

    private static PdfRenderEngine defaultRenderEngine() {
        return new PdfBoxRenderEngine();
    }

    private static BuildPdfOcrTextLayerUseCase defaultOcrTextLayer(PdfRenderEngine renderEngine) {
        PdfRenderEngine engine = renderEngine == null ? defaultRenderEngine() : renderEngine;
        Path root = RuntimePathResolver.defaultResolver().resolve().layout().applicationRoot();
        TesseractRuntimeLocator locator = new TesseractRuntimeLocator();
        return new BuildPdfOcrTextLayerUseCase(new TesseractPdfOcrEngine(engine, new DefaultExternalProcessRunner(),
                () -> locator.locate(OperationalSettings.defaults(), root).command()));
    }

    @Override
    public ReadableDocument importDocument(Path sourceFile) throws IOException {
        validatePdfHeader(sourceFile);
        PdfDocumentInfo visualInfo = inspectVisual(sourceFile);
        int pageCount = Math.max(1, visualInfo.pageCount() > 0 ? visualInfo.pageCount() : safeCountPdfPages(sourceFile));
        List<DocumentImportIssue> issues = new ArrayList<>();
        issues.add(DocumentImportIssue.info("read-only-source", "PDF importado como fuente solo lectura; las capas viven en el proyecto."));
        addVisualRenderIssues(issues, visualInfo);
        issues.add(DocumentImportIssue.warning("pdf-ocr-deferred",
                "PDF abierto en modo OCR progresivo: los fragmentos narrables se agregaran por pagina visible."));
        issues.add(DocumentImportIssue.warning("pdf-visual-only",
                "PDF abierto de inmediato en modo visual; hover, seleccion y audio usaran OCR local por pagina."));
        return new ReadableDocument(
                PlainTextDocumentImporter.titleFrom(sourceFile),
                SourceDocumentFormat.PDF,
                sourceFile,
                blocksFromVisualFallback(sourceFile, pageCount, visualInfo.visuallyRenderable()),
                new DocumentImportReport(issues));
    }

    private static void validatePdfHeader(Path sourceFile) throws IOException {
        byte[] header = new byte[8];
        int read;
        try (InputStream in = Files.newInputStream(sourceFile)) {
            read = in.read(header);
        }
        String text = read <= 0 ? "" : new String(header, 0, read, StandardCharsets.ISO_8859_1);
        if (!text.contains("%PDF")) {
            throw new IOException("El archivo seleccionado no parece ser un PDF valido.");
        }
    }

    public boolean hasNativeText(Path sourceFile) throws IOException {
        return hasEnoughNativeText(extractNativeText(sourceFile));
    }

    static String extractNativeText(Path sourceFile) throws IOException {
        return extractNativeTextReport(sourceFile, 0).normalizedText();
    }

    private PdfDocumentInfo inspectVisual(Path sourceFile) throws IOException {
        try {
            return renderEngine.inspect(sourceFile, PdfOpenOptions.empty());
        } catch (PdfRenderException ex) {
            if (ex.code() == PdfRenderErrorCode.PASSWORD_REQUIRED) {
                throw ex;
            }
            return PdfDocumentInfo.unavailable(sourceFile, safeCountPdfPages(sourceFile), ex.code().name());
        }
    }

    private static PdfExtraction extractNativeTextReport(Path sourceFile, int rendererPageCount) throws IOException {
        byte[] pdf = Files.readAllBytes(sourceFile);
        if (pdf.length == 0 || !new String(pdf, 0, Math.min(pdf.length, 8), StandardCharsets.ISO_8859_1).contains("%PDF")) {
            throw new IOException("El archivo seleccionado no parece ser un PDF valido.");
        }
        String rawPdf = new String(pdf, StandardCharsets.ISO_8859_1);
        int pageCount = Math.max(countPdfPages(rawPdf), rendererPageCount);
        Optional<PdfExtraction> bbox = extractWithPopplerBbox(sourceFile, pageCount);
        if (bbox.isPresent() && hasEnoughNativeText(bbox.get().normalizedText())) {
            return bbox.get();
        }
        Optional<PdfExtraction> poppler = extractWithPopplerLayout(sourceFile, pageCount);
        if (poppler.isPresent() && hasEnoughNativeText(poppler.get().normalizedText())) {
            return poppler.get();
        }

        StringBuilder extracted = new StringBuilder();
        List<byte[]> streams = extractStreams(pdf);
        for (byte[] stream : streams) {
            extracted.append(' ').append(extractTextOperators(new String(stream, StandardCharsets.ISO_8859_1)));
        }
        if (extracted.length() < MIN_NATIVE_TEXT_CHARACTERS) {
            extracted.append(' ').append(extractTextOperators(rawPdf));
        }
        PdfExtraction fallback = repairPageText(List.of(extracted.toString()), "java-native", pageCount);
        if (hasEnoughNativeText(fallback.normalizedText()) || poppler.isEmpty()) {
            return fallback;
        }
        return poppler.get();
    }

    private OcrImportResult buildOcrBlocks(Path sourceFile, int pageCount, boolean visualRenderable) {
        if (ocrTextLayer == null) {
            return new OcrImportResult(List.of(), List.of(DocumentImportIssue.warning("pdf-ocr-unavailable",
                    "OCR local no configurado; se conserva el PDF como visual.")));
        }
        int safePageCount = Math.max(1, pageCount);
        List<DocumentBlock> blocks = new ArrayList<>();
        List<DocumentImportIssue> issues = new ArrayList<>();
        int index = 1;
        for (int page = 1; page <= safePageCount; page++) {
            int currentPage = page;
            try {
                PdfOcrPageResult result = ocrTextLayer.recognize(new PdfOcrRequest(
                        sourceFile,
                        currentPage,
                        PdfOcrRequest.DEFAULT_DPI,
                        PdfOcrRequest.DEFAULT_MAX_PIXEL_COUNT,
                        PdfOcrRequest.DEFAULT_LANGUAGES,
                        false,
                        defaultOcrCacheDirectory()));
                List<DocumentBlock> pageBlocks = ocrBlockMapper.blocksFromOcrLayer(
                        result.textLayer(), safePageCount, visualRenderable, index);
                blocks.addAll(pageBlocks);
                index += pageBlocks.size();
                result.warnings().forEach(warning -> issues.add(DocumentImportIssue.warning("pdf-ocr-page-warning",
                        "OCR p. " + currentPage + ": " + warning)));
            } catch (PdfOcrException ex) {
                issues.add(DocumentImportIssue.warning("pdf-ocr-unavailable",
                        "OCR p. " + currentPage + ": [" + ex.code() + "] " + ex.getMessage()));
            }
        }
        if (!blocks.isEmpty()) {
            issues.add(DocumentImportIssue.info("pdf-ocr-applied",
                    "Texto OCR local detectado y convertido en fragmentos narrables."));
        }
        return new OcrImportResult(List.copyOf(blocks), List.copyOf(issues));
    }

    private static Path defaultOcrCacheDirectory() {
        return Path.of(System.getProperty("java.io.tmpdir"), "docupodcast-studio", "pdf-ocr");
    }

    private static Optional<PdfExtraction> extractWithPopplerBbox(Path sourceFile, int fallbackPageCount) {
        try {
            ExternalProcessResult result = new DefaultExternalProcessRunner().run(
                    ExternalProcessRequest.of(List.of("pdftotext", "-bbox-layout", sourceFile.toString(), "-"),
                            "pdf-poppler-bbox-layout", Duration.ofSeconds(18)));
            if (!result.succeeded()) {
                return Optional.empty();
            }
            PdfBboxExtraction parsed = new PdfBboxLayoutParser().parse(result.stdout().replace("\uFEFF", ""));
            return Optional.of(repairBboxText(parsed, Math.max(fallbackPageCount, parsed.pageCount())));
        } catch (IOException | InterruptedException | RuntimeException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }

    private static Optional<PdfExtraction> extractWithPopplerLayout(Path sourceFile, int fallbackPageCount) {
        try {
            ExternalProcessResult result = new DefaultExternalProcessRunner().run(
                    ExternalProcessRequest.of(List.of("pdftotext", "-layout", sourceFile.toString(), "-"),
                            "pdf-poppler-layout", Duration.ofSeconds(12)));
            if (!result.succeeded()) {
                return Optional.empty();
            }
            String text = result.stdout().replace("\uFEFF", "");
            List<String> pages = splitRawPages(text);
            return Optional.of(repairPageText(pages, "poppler-layout", Math.max(fallbackPageCount, pages.size())));
        } catch (IOException | InterruptedException | RuntimeException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }

    private static PdfExtraction repairBboxText(PdfBboxExtraction extraction, int fallbackPageCount) {
        PdfBboxExtraction safeExtraction = extraction == null ? new PdfBboxExtraction(List.of()) : extraction;
        ArrayList<PdfPageText> repairedPages = new ArrayList<>();
        ArrayList<PdfBboxTextBlock> repairedBlocks = new ArrayList<>();
        int letterSpacing = 0;
        int artificialLineBreaks = 0;
        int repeatedHeaderFooter = 0;
        for (PdfBboxPage page : safeExtraction.pages()) {
            PdfTextNormalizationReport pageReport = TEXT_REPAIR.repair(page.rawText());
            letterSpacing += pageReport.letterSpacingRepairs();
            artificialLineBreaks += pageReport.artificialLineBreakRepairs();
            repeatedHeaderFooter += pageReport.repeatedHeaderFooterLinesRemoved();
            if (!pageReport.normalizedText().isBlank()) {
                repairedPages.add(new PdfPageText(page.pageNumber(), pageReport.normalizedText()));
            }
            for (PdfBboxTextBlock block : page.blocks()) {
                PdfTextNormalizationReport blockReport = TEXT_REPAIR.repair(block.text());
                if (!blockReport.normalizedText().isBlank()) {
                    repairedBlocks.add(new PdfBboxTextBlock(block.pageNumber(), blockReport.normalizedText(),
                            block.bbox(), block.pageWidth(), block.pageHeight()));
                }
            }
        }
        String joined = repairedPages.stream()
                .map(PdfPageText::text)
                .collect(java.util.stream.Collectors.joining("\n\n"));
        PdfTextNormalizationReport combined = new PdfTextNormalizationReport(
                joined, letterSpacing, artificialLineBreaks, repeatedHeaderFooter);
        return new PdfExtraction(
                List.copyOf(repairedPages),
                combined,
                "poppler-bbox-layout",
                Math.max(1, Math.max(fallbackPageCount, safeExtraction.pageCount())),
                List.copyOf(repairedBlocks));
    }

    private static PdfExtraction repairPageText(List<String> rawPages, String extractionMode, int fallbackPageCount) {
        List<String> pages = rawPages == null || rawPages.isEmpty() ? List.of("") : rawPages;
        ArrayList<PdfPageText> repairedPages = new ArrayList<>();
        int letterSpacing = 0;
        int artificialLineBreaks = 0;
        int repeatedHeaderFooter = 0;
        for (int i = 0; i < pages.size(); i++) {
            PdfTextNormalizationReport report = TEXT_REPAIR.repair(pages.get(i));
            letterSpacing += report.letterSpacingRepairs();
            artificialLineBreaks += report.artificialLineBreakRepairs();
            repeatedHeaderFooter += report.repeatedHeaderFooterLinesRemoved();
            if (!report.normalizedText().isBlank()) {
                repairedPages.add(new PdfPageText(i + 1, report.normalizedText()));
            }
        }
        String joined = repairedPages.stream()
                .map(PdfPageText::text)
                .collect(java.util.stream.Collectors.joining("\n\n"));
        PdfTextNormalizationReport combined = new PdfTextNormalizationReport(
                joined, letterSpacing, artificialLineBreaks, repeatedHeaderFooter);
        return new PdfExtraction(
                List.copyOf(repairedPages),
                combined,
                extractionMode,
                Math.max(1, Math.max(fallbackPageCount, pages.size())),
                List.of());
    }

    private static List<String> splitRawPages(String text) {
        String safe = text == null ? "" : text;
        String[] split = safe.split("\\f", -1);
        ArrayList<String> pages = new ArrayList<>();
        for (String page : split) {
            pages.add(page == null ? "" : page);
        }
        return pages.isEmpty() ? List.of("") : List.copyOf(pages);
    }

    private static List<DocumentBlock> blocksFromPdfText(PdfExtraction extraction, boolean visualRenderable) {
        if (!extraction.bboxBlocks().isEmpty()) {
            return blocksFromPdfBboxText(extraction, visualRenderable);
        }
        List<DocumentBlock> blocks = new ArrayList<>();
        int index = 1;
        for (PdfPageText page : extraction.pages()) {
            String[] paragraphs = page.text().replace("\r\n", "\n").split("\\n\\s*\\n|(?<=\\.)\\s+(?=[A-Z])");
            for (String paragraph : paragraphs) {
                String normalized = paragraph == null ? "" : paragraph.strip();
                if (PdfNarratableTextClassifier.shouldSkip(normalized)) {
                    continue;
                }
                DocumentBlockType type = classifyPdfBlock(normalized, index);
                String blockId = PlainTextDocumentImporter.blockId(index);
                Map<String, String> metadata = new LinkedHashMap<>();
                metadata.put("sourceMode", "read-only");
                metadata.put("sourceFormat", SourceDocumentFormat.PDF.name());
                metadata.put("pdfTextMapVersion", "1");
                metadata.put("nativeText", "true");
                metadata.put("ocr", "false");
                metadata.put("sourcePage", Integer.toString(page.pageNumber()));
                metadata.put("sourcePageCount", Integer.toString(extraction.pageCount()));
                metadata.put("visualRenderAvailable", Boolean.toString(visualRenderable));
                metadata.put("visualRenderEngine", visualRenderable ? "pdfbox" : "");
                metadata.put("bbox", "");
                metadata.put("bboxUnits", "");
                metadata.put("pageWidth", "");
                metadata.put("pageHeight", "");
                metadata.put("extractionMode", extraction.extractionMode());
                metadata.put("confidence", "native-text");
                metadata.put("sourceLocatorLabel", "PDF nativo - pagina " + page.pageNumber() + " - bloque " + blockId);
                blocks.add(DocumentBlock.of(blockId, type, normalized, "PDF text", metadata));
                index++;
            }
        }
        return blocks;
    }

    private static List<DocumentBlock> blocksFromPdfBboxText(PdfExtraction extraction, boolean visualRenderable) {
        List<DocumentBlock> blocks = new ArrayList<>();
        int index = 1;
        for (PdfBboxTextBlock sourceBlock : extraction.bboxBlocks()) {
            String normalized = sourceBlock.text() == null ? "" : sourceBlock.text().strip();
            if (PdfNarratableTextClassifier.shouldSkip(normalized)) {
                continue;
            }
            DocumentBlockType type = classifyPdfBlock(normalized, index);
            String blockId = PlainTextDocumentImporter.blockId(index);
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("sourceMode", "read-only");
            metadata.put("sourceFormat", SourceDocumentFormat.PDF.name());
            metadata.put("pdfTextMapVersion", "1");
            metadata.put("nativeText", "true");
            metadata.put("ocr", "false");
            metadata.put("sourcePage", Integer.toString(sourceBlock.pageNumber()));
            metadata.put("sourcePageCount", Integer.toString(extraction.pageCount()));
            metadata.put("visualRenderAvailable", Boolean.toString(visualRenderable));
            metadata.put("visualRenderEngine", visualRenderable ? "pdfbox" : "");
            metadata.put("bbox", sourceBlock.bbox().compact());
            metadata.put("bboxUnits", "pdf-points");
            metadata.put("pageWidth", sourceBlock.pageWidthLabel());
            metadata.put("pageHeight", sourceBlock.pageHeightLabel());
            metadata.put("extractionMode", extraction.extractionMode());
            metadata.put("confidence", "native-text");
            metadata.put("sourceLocatorLabel", "PDF nativo - pagina " + sourceBlock.pageNumber() + " - bloque " + blockId);
            blocks.add(DocumentBlock.of(blockId, type, normalized, "PDF bbox text", metadata));
            index++;
        }
        return blocks;
    }

    private static List<DocumentBlock> blocksFromVisualFallback(Path sourceFile, int pageCount, boolean visualRenderable) {
        int safePages = Math.max(1, pageCount);
        List<DocumentBlock> blocks = new ArrayList<>();
        for (int page = 1; page <= safePages; page++) {
            String blockId = PlainTextDocumentImporter.blockId(page);
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("sourceMode", "read-only");
            metadata.put("sourceFormat", SourceDocumentFormat.PDF.name());
            metadata.put("nativeText", "false");
            metadata.put("ocr", "false");
            metadata.put("visualBlock", "true");
            metadata.put("sourcePage", Integer.toString(page));
            metadata.put("sourcePageCount", Integer.toString(safePages));
            metadata.put("visualRenderAvailable", Boolean.toString(visualRenderable));
            metadata.put("visualRenderEngine", visualRenderable ? "pdfbox" : "");
            metadata.put("bbox", "");
            metadata.put("bboxUnits", "");
            metadata.put("pageWidth", "");
            metadata.put("pageHeight", "");
            metadata.put("extractionMode", "visual-fallback");
            metadata.put("confidence", "visual-only");
            metadata.put("sourceLocatorLabel", "PDF visual - pagina " + page + " (OCR local pendiente)");
            blocks.add(DocumentBlock.of(blockId, DocumentBlockType.IMAGE_NOTICE,
                    "Pagina " + page + " del PDF visual " + sourceFile.getFileName() + ". OCR local pendiente.",
                    "PDF visual", metadata));
        }
        return blocks;
    }

    private static DocumentBlockType classifyPdfBlock(String text, int index) {
        return PdfNarratableTextClassifier.classify(text, index);
    }

    private static boolean hasEnoughNativeText(String text) {
        String normalized = text == null ? "" : text.strip();
        if (normalized.length() < MIN_NATIVE_TEXT_CHARACTERS) {
            return false;
        }
        long words = java.util.Arrays.stream(normalized.split("\\s+"))
                .filter(token -> token.chars().anyMatch(Character::isLetter))
                .count();
        return words >= MIN_NATIVE_TEXT_WORDS;
    }

    private static void addNormalizationIssues(List<DocumentImportIssue> issues, PdfTextNormalizationReport report) {
        if (report == null || !report.repaired()) {
            return;
        }
        issues.add(DocumentImportIssue.info("pdf-text-normalized",
                "Texto PDF normalizado de forma prudente antes de construir fragmentos."));
        if (report.letterSpacingRepairs() > 0) {
            issues.add(DocumentImportIssue.info("pdf-letter-spacing-repaired",
                    "Se repararon " + report.letterSpacingRepairs() + " palabra(s) con letras espaciadas."));
        }
        if (report.artificialLineBreakRepairs() > 0) {
            issues.add(DocumentImportIssue.info("pdf-line-breaks-repaired",
                    "Se unieron " + report.artificialLineBreakRepairs() + " salto(s) artificiales de linea."));
        }
        if (report.repeatedHeaderFooterLinesRemoved() > 0) {
            issues.add(DocumentImportIssue.info("pdf-repeated-header-footer-removed",
                    "Se omitieron " + report.repeatedHeaderFooterLinesRemoved()
                            + " linea(s) repetidas tipo encabezado o pie."));
        }
    }

    private static void addVisualRenderIssues(List<DocumentImportIssue> issues, PdfDocumentInfo visualInfo) {
        if (visualInfo == null || !visualInfo.visuallyRenderable()) {
            issues.add(DocumentImportIssue.warning("pdf-embedded-renderer-unavailable",
                    "El render visual embebido no pudo inspeccionar este PDF; se conservan fallbacks existentes."));
            return;
        }
        issues.add(DocumentImportIssue.info("pdf-embedded-renderer-available",
                "Render visual PDF embebido disponible con PDFBox para paginas, crops y visor futuro."));
    }

    private static int safeCountPdfPages(Path sourceFile) {
        try {
            return countPdfPages(Files.readString(sourceFile, StandardCharsets.ISO_8859_1));
        } catch (IOException | RuntimeException ex) {
            return 1;
        }
    }

    private static int countPdfPages(String raw) {
        if (raw == null || raw.isBlank()) {
            return 1;
        }
        int count = 0;
        int position = 0;
        while ((position = raw.indexOf("/Type", position)) >= 0) {
            int page = raw.indexOf("/Page", position);
            if (page >= 0 && page - position < 32) {
                int after = page + "/Page".length();
                boolean pagesNode = after < raw.length() && Character.isLetter(raw.charAt(after));
                if (!pagesNode) {
                    count++;
                }
                position = after;
            } else {
                position += "/Type".length();
            }
        }
        return Math.max(1, count);
    }

    private static List<byte[]> extractStreams(byte[] pdf) {
        List<byte[]> streams = new ArrayList<>();
        String raw = new String(pdf, StandardCharsets.ISO_8859_1);
        int position = 0;
        while (position >= 0 && position < raw.length()) {
            int streamToken = raw.indexOf("stream", position);
            if (streamToken < 0) {
                break;
            }
            int dataStart = streamToken + "stream".length();
            if (dataStart < raw.length() && raw.charAt(dataStart) == '\r') {
                dataStart++;
            }
            if (dataStart < raw.length() && raw.charAt(dataStart) == '\n') {
                dataStart++;
            }
            int endStream = raw.indexOf("endstream", dataStart);
            if (endStream < 0) {
                break;
            }
            byte[] data = java.util.Arrays.copyOfRange(pdf, dataStart, endStream);
            String dictionary = raw.substring(Math.max(0, streamToken - 800), streamToken).toLowerCase(Locale.ROOT);
            if (dictionary.contains("/flatedecode")) {
                streams.add(inflateOrOriginal(data));
            } else {
                streams.add(data);
            }
            position = endStream + "endstream".length();
        }
        return streams;
    }

    private static byte[] inflateOrOriginal(byte[] data) {
        try (InflaterInputStream inflater = new InflaterInputStream(new ByteArrayInputStream(data));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            inflater.transferTo(out);
            return out.toByteArray();
        } catch (IOException | RuntimeException ex) {
            return data;
        }
    }

    private static String extractTextOperators(String content) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char current = content.charAt(i);
            if (current == '(') {
                ParseResult literal = parseLiteralString(content, i);
                if (!literal.value().isBlank() && isProbablyText(literal.value())) {
                    text.append(literal.value()).append(' ');
                }
                i = literal.endIndex();
            } else if (current == '<' && i + 1 < content.length() && content.charAt(i + 1) != '<') {
                ParseResult hex = parseHexString(content, i);
                if (!hex.value().isBlank() && isProbablyText(hex.value())) {
                    text.append(hex.value()).append(' ');
                }
                i = hex.endIndex();
            }
        }
        return text.toString();
    }

    private static ParseResult parseLiteralString(String content, int start) {
        StringBuilder value = new StringBuilder();
        int depth = 0;
        boolean escaping = false;
        int i = start;
        for (; i < content.length(); i++) {
            char c = content.charAt(i);
            if (i == start) {
                depth = 1;
                continue;
            }
            if (escaping) {
                value.append(switch (c) {
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    case 'b' -> '\b';
                    case 'f' -> '\f';
                    default -> c;
                });
                escaping = false;
                continue;
            }
            if (c == '\\') {
                escaping = true;
                continue;
            }
            if (c == '(') {
                depth++;
                value.append(c);
                continue;
            }
            if (c == ')') {
                depth--;
                if (depth == 0) {
                    break;
                }
                value.append(c);
                continue;
            }
            value.append(c);
        }
        return new ParseResult(value.toString(), Math.min(i, content.length() - 1));
    }

    private static ParseResult parseHexString(String content, int start) {
        int end = content.indexOf('>', start + 1);
        if (end < 0) {
            return new ParseResult("", start);
        }
        String hex = content.substring(start + 1, end).replaceAll("\\s+", "");
        if (hex.length() < 2 || hex.length() % 2 != 0) {
            return new ParseResult("", end);
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            try {
                bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            } catch (NumberFormatException ex) {
                return new ParseResult("", end);
            }
        }
        String decoded;
        if (bytes.length >= 2 && bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
            decoded = new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE);
        } else {
            decoded = new String(bytes, StandardCharsets.ISO_8859_1);
        }
        return new ParseResult(decoded, end);
    }

    private static boolean isProbablyText(String value) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.length() < 2) {
            return false;
        }
        long letters = normalized.chars().filter(Character::isLetter).count();
        long controls = normalized.chars().filter(ch -> Character.isISOControl(ch) && !Character.isWhitespace(ch)).count();
        return letters >= 2 && controls == 0;
    }

    private record OcrImportResult(List<DocumentBlock> blocks, List<DocumentImportIssue> issues) {
        private OcrImportResult {
            blocks = blocks == null ? List.of() : List.copyOf(blocks);
            issues = issues == null ? List.of() : List.copyOf(issues);
        }
    }

    private record PdfExtraction(
            List<PdfPageText> pages,
            PdfTextNormalizationReport report,
            String extractionMode,
            int pageCount,
            List<PdfBboxTextBlock> bboxBlocks
    ) {
        private PdfExtraction {
            pages = pages == null ? List.of() : List.copyOf(pages);
            report = report == null ? new PdfTextNormalizationReport("", 0, 0, 0) : report;
            extractionMode = extractionMode == null || extractionMode.isBlank() ? "unknown" : extractionMode.strip();
            pageCount = Math.max(1, pageCount);
            bboxBlocks = bboxBlocks == null ? List.of() : List.copyOf(bboxBlocks);
        }

        String normalizedText() {
            return report.normalizedText();
        }
    }

    private record PdfPageText(int pageNumber, String text) {
        private PdfPageText {
            pageNumber = Math.max(1, pageNumber);
            text = text == null ? "" : text.strip();
        }
    }

    private record ParseResult(String value, int endIndex) {
    }
}
