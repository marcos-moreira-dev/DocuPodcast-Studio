package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.*;
import com.marcosmoreiradev.docupodcaststudio.application.script.BuildPreparedPdfNarrationUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Reproducible PDFBox corpus exercising the final V2-only lifecycle. */
final class PdfV2CorpusEndToEndTest {
    @TempDir Path temp;

    @Test
    void importsPreparesReopensQueriesNarratesDiagnosesAndRendersCorpus() throws Exception {
        List<CorpusCase> cases = List.of(
                new CorpusCase("digital", "DIGITAL_FIABLE", false, false),
                new CorpusCase("scanned", "ESCANEADO_OCR", true, false),
                new CorpusCase("hybrid", "HIBRIDO_EVIDENCIA", true, true),
                new CorpusCase("rotated", "ROTADO_GEOMETRIA", false, false),
                new CorpusCase("multicolumn", "MULTICOLUMNA_ORDEN", false, false),
                new CorpusCase("table", "TABLA_BUSCABLE", true, false),
                new CorpusCase("formula", "FORMULA_BUSCABLE", true, false),
                new CorpusCase("defective", "GALIMATIA_DUDOSA", true, false));
        Path sourceDirectory = Files.createDirectories(temp.resolve("fuentes-ñ"));
        Path evidenceDirectory = Files.createDirectories(
                Path.of("target/pdf-v2-corpus").toAbsolutePath().normalize());
        JsonPreparedPdfDocumentRepository preparedRepository =
                new JsonPreparedPdfDocumentRepository();
        CountingOcrEngine ocr = new CountingOcrEngine();
        PdfNativePageExtractor nativeExtractor = this::nativeLayer;
        PreparePdfPageUseCase prepare = new PreparePdfPageUseCase(
                new BuildPdfOcrTextLayerUseCase(ocr), new PdfTextLayerBlockMapper(),
                OperationalSettings.OcrSettings::defaults, preparedRepository, nativeExtractor);
        PdfBoxRenderEngine renderEngine = new PdfBoxRenderEngine();
        CreatePreparedPdfSessionWorkspaceUseCase importPdf =
                new CreatePreparedPdfSessionWorkspaceUseCase(preparedRepository,
                        new BuildPdfVisualDocumentUseCase(renderEngine));
        ReadableDocumentWorkspaceRepository workspaceRepository =
                new ReadableDocumentWorkspaceRepository(preparedRepository);
        ArrayList<String> reportCases = new ArrayList<>();
        ThreadMXBean threadMetrics = ManagementFactory.getThreadMXBean();
        long suiteStartedAt = System.nanoTime();
        long suiteCpuStartedAt = currentThreadCpuNanos(threadMetrics);
        long peakObservedHeapBytes = usedHeapBytes();
        long totalPreparedBytes = 0;
        long totalPreparedFiles = 0;
        int totalRegions = 0;
        int totalUncertainRegions = 0;

        for (CorpusCase corpusCase : cases) {
            long caseStartedAt = System.nanoTime();
            long caseCpuStartedAt = currentThreadCpuNanos(threadMetrics);
            int ocrCallsBefore = ocr.totalCalls.get();
            Path source = sourceDirectory.resolve(corpusCase.id() + "-á.pdf");
            writeFixture(source, corpusCase);
            String originalHash = sha256(source);
            PreparedPdfSource staged = importPdf.create(source, corpusCase.id());
            assertTrue(preparedRepository.listPreparedPageNumbers(
                    staged.workspace().projectRoot()).isEmpty());

            PreparePdfPageResult prepared = prepare.execute(new PreparePdfPageRequest(
                    staged.workspace(), temp.resolve("cache"), 1, false, null));
            assertTrue(prepared.succeeded(), () -> corpusCase.id() + ": " + prepared.issues());
            assertFalse(prepared.preparedPage().regions().isEmpty(), corpusCase.id());
            assertEquals(originalHash, sha256(source), "El original externo no debe cambiar");

            Path projectRoot = temp.resolve("proyectos").resolve(corpusCase.id());
            var materialized = workspaceRepository.materialize(staged,
                    ReadingProfile.academicDefaults(),
                    projectRoot.resolve(corpusCase.id() + ".docupodcast.json"));
            PreparedPdfSource reopened = (PreparedPdfSource) workspaceRepository
                    .load(projectRoot.resolve(corpusCase.id() + ".docupodcast.json"))
                    .orElseThrow();
            assertInstanceOf(PreparedPdfSource.class, materialized.projectSource());
            assertFalse(Files.exists(projectRoot.resolve("document/document.json")));

            PdfTextSearchProjection search = new SearchPdfTextUseCase(preparedRepository).search(
                    new PdfTextSearchRequest(reopened.workspace(), corpusCase.token(),
                            false, 20, 0));
            assertFalse(search.results().isEmpty(), "Debe poder buscar " + corpusCase.token());
            PdfRegionSelectionRef selection = new PdfRegionSelectionRef(1,
                    search.results().getFirst().regionId(), 0,
                    search.results().getFirst().snippet().length());
            assertTrue(new ResolveDocumentSelectionUseCase(preparedRepository)
                    .resolve(reopened, selection).isPresent());

            var script = new BuildPreparedPdfNarrationUseCase(preparedRepository).build(
                    reopened.workspace(), corpusCase.id(), "es", false,
                    ReadingProfile.academicDefaults());
            assertTrue(script.segments().stream().allMatch(segment ->
                            PdfNarrationBindingMetadata.decode(segment.metadata().get(
                                    PdfNarrationBindingMetadata.KEY)).isPresent()),
                    "Cada segmento PDF debe conservar su binding narrativo");
            for (var region : prepared.preparedPage().regions()) {
                if (region.effectiveNarratability() == PdfNarratability.UNCERTAIN) {
                    assertTrue(script.segments().stream().noneMatch(segment ->
                            segment.sourceBlockIds().contains(region.id())));
                }
            }
            if (corpusCase.hybrid()) {
                assertTrue(prepared.preparedPage().regions().stream()
                        .anyMatch(region -> region.evidenceCandidates().size() >= 2),
                        "La página híbrida debe conservar ambas evidencias");
            }

            PdfWorkspaceDiagnosticReport diagnostic =
                    new DiagnosePreparedPdfWorkspaceUseCase(preparedRepository)
                            .diagnose(projectRoot, temp.resolve("cache"));
            assertTrue(diagnostic.manifestValid());
            assertEquals(1, diagnostic.preparedPages());
            renderOverlay(renderEngine, reopened, prepared.preparedPage(),
                    evidenceDirectory.resolve(corpusCase.id() + "-overlay.png"));
            long preparedFiles = countRegularFiles(projectRoot.resolve("document"));
            long elapsedMillis = nanosToMillis(System.nanoTime() - caseStartedAt);
            long cpuMillis = nanosToMillis(
                    currentThreadCpuNanos(threadMetrics) - caseCpuStartedAt);
            long heapBytes = usedHeapBytes();
            int ocrCalls = ocr.totalCalls.get() - ocrCallsBefore;
            peakObservedHeapBytes = Math.max(peakObservedHeapBytes, heapBytes);
            totalPreparedBytes += diagnostic.preparedBytes();
            totalPreparedFiles += preparedFiles;
            totalRegions += diagnostic.regions();
            totalUncertainRegions += diagnostic.uncertainRegions();
            reportCases.add("{\"id\":\"" + corpusCase.id() + "\",\"ocr\":"
                    + (ocrCalls > 0) + ",\"ocrCalls\":" + ocrCalls
                    + ",\"elapsedMillis\":" + elapsedMillis
                    + ",\"cpuMillis\":" + cpuMillis
                    + ",\"observedHeapBytes\":" + heapBytes
                    + ",\"preparedFiles\":" + preparedFiles
                    + ",\"regions\":" + diagnostic.regions()
                    + ",\"uncertain\":" + diagnostic.uncertainRegions()
                    + ",\"bytes\":" + diagnostic.preparedBytes() + "}");
        }
        assertEquals(0, ocr.callsFor("digital"));
        assertTrue(ocr.totalCalls.get() >= 4);
        long suiteElapsedMillis = nanosToMillis(System.nanoTime() - suiteStartedAt);
        long suiteCpuMillis = nanosToMillis(
                currentThreadCpuNanos(threadMetrics) - suiteCpuStartedAt);
        double ocrRate = cases.isEmpty() ? 0 : (double) ocr.totalCalls.get() / cases.size();
        Files.writeString(evidenceDirectory.resolve("pdf-v2-corpus-report.json"),
                "{\"schemaVersion\":2,\"informativeBaseline\":true,\"summary\":{"
                        + "\"pages\":" + cases.size()
                        + ",\"elapsedMillis\":" + suiteElapsedMillis
                        + ",\"cpuMillis\":" + suiteCpuMillis
                        + ",\"peakObservedHeapBytes\":" + peakObservedHeapBytes
                        + ",\"preparedBytes\":" + totalPreparedBytes
                        + ",\"preparedFiles\":" + totalPreparedFiles
                        + ",\"ocrPages\":" + ocr.totalCalls.get()
                        + ",\"ocrRate\":" + String.format(Locale.ROOT, "%.4f", ocrRate)
                        + ",\"regions\":" + totalRegions
                        + ",\"uncertainRegions\":" + totalUncertainRegions
                        + "},\"cases\":[" + String.join(",", reportCases) + "]}",
                StandardCharsets.UTF_8);
    }

    private static long currentThreadCpuNanos(ThreadMXBean metrics) {
        if (!metrics.isCurrentThreadCpuTimeSupported()) {
            return 0;
        }
        if (!metrics.isThreadCpuTimeEnabled()) {
            try {
                metrics.setThreadCpuTimeEnabled(true);
            } catch (UnsupportedOperationException | SecurityException ignored) {
                return 0;
            }
        }
        return Math.max(0, metrics.getCurrentThreadCpuTime());
    }

    private static long nanosToMillis(long nanos) {
        return Math.max(0, nanos) / 1_000_000;
    }

    private static long usedHeapBytes() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static long countRegularFiles(Path directory) throws Exception {
        if (!Files.isDirectory(directory)) {
            return 0;
        }
        try (var files = Files.walk(directory)) {
            return files.filter(Files::isRegularFile).count();
        }
    }

    private PdfTextLayer nativeLayer(Path source, int page) {
        String id = source.getFileName().toString().toLowerCase(Locale.ROOT);
        if (id.contains("scanned") || id.contains("defective")
                || id.contains("table") || id.contains("formula")) {
            return new PdfTextLayer(page, PdfTextLayerOrigin.UNAVAILABLE, List.of(),
                    List.of("Sin texto nativo fiable."));
        }
        String text;
        if (id.contains("hybrid")) {
            text = "HIBRIDO_EVIDENCIA texto nativo dañado \uFFFD conserva evidencia contradictoria.";
        } else if (id.contains("multicolumn")) {
            String left = "MULTICOLUMNA_ORDEN primera columna con suficiente prosa coherente.";
            String right = "Segunda columna también conserva geometría y orden de lectura.";
            PdfPageRegion leftBox = new PdfPageRegion(page, 45, 80, 285, 190, 612, 792);
            PdfPageRegion rightBox = new PdfPageRegion(page, 325, 80, 570, 190, 612, 792);
            return new PdfTextLayer(page, PdfTextLayerOrigin.NATIVE_BBOX,
                    List.of(
                            new PdfTextLine(page, left, leftBox,
                                    List.of(new PdfTextToken(left, leftBox, 0.96)), 0.96),
                            new PdfTextLine(page, right, rightBox,
                                    List.of(new PdfTextToken(right, rightBox, 0.96)), 0.96)),
                    List.of());
        } else if (id.contains("rotated")) {
            text = "ROTADO_GEOMETRIA página rotada con texto nativo suficiente y geometría válida.";
        } else {
            text = "DIGITAL_FIABLE documento digital con texto nativo suficiente para evitar "
                    + "completamente el proceso local de reconocimiento óptico.";
        }
        PdfPageRegion box = new PdfPageRegion(page, 60, 80, 540, 150, 612, 792);
        PdfTextToken token = new PdfTextToken(text, box, 0.96);
        return new PdfTextLayer(page, PdfTextLayerOrigin.NATIVE_BBOX,
                List.of(new PdfTextLine(page, text, box, List.of(token), 0.96)), List.of());
    }

    private static void renderOverlay(PdfBoxRenderEngine engine, PreparedPdfSource source,
                                      com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage page,
                                      Path output) throws Exception {
        PdfPageRenderResult rendered = engine.renderPage(new PdfPageRenderRequest(
                source.sourcePath(), 1, 120, PdfPageRenderRequest.DEFAULT_MAX_PIXEL_COUNT,
                Color.WHITE, true));
        BufferedImage image = rendered.image();
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(220, 35, 60, 180));
            graphics.setStroke(new BasicStroke(3f));
            double scaleX = image.getWidth() / page.widthPoints();
            double scaleY = image.getHeight() / page.heightPoints();
            for (var region : page.regions()) {
                graphics.drawRect((int) Math.round(region.xMin() * scaleX),
                        (int) Math.round(region.yMin() * scaleY),
                        Math.max(1, (int) Math.round((region.xMax() - region.xMin()) * scaleX)),
                        Math.max(1, (int) Math.round((region.yMax() - region.yMin()) * scaleY)));
            }
        } finally {
            graphics.dispose();
        }
        ImageIO.write(image, "png", output.toFile());
        assertTrue(Files.size(output) > 0);
    }

    private static void writeFixture(Path target, CorpusCase corpusCase) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            if (corpusCase.id().equals("rotated")) page.setRotation(90);
            document.addPage(page);
            if (corpusCase.id().equals("scanned") || corpusCase.id().equals("defective")) {
                BufferedImage image = new BufferedImage(1200, 400, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics();
                try {
                    graphics.setColor(Color.WHITE);
                    graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
                    graphics.setColor(Color.BLACK);
                    graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 60));
                    graphics.drawString(corpusCase.token(), 60, 220);
                } finally {
                    graphics.dispose();
                }
                var pdfImage = LosslessFactory.createFromImage(document, image);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.drawImage(pdfImage, 50, 320, 510, 170);
                }
            } else {
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(60, 700);
                    content.showText(corpusCase.token().replace('Í', 'I'));
                    content.endText();
                    if (corpusCase.id().equals("table")) {
                        for (int x : new int[]{60, 260, 460}) content.moveTo(x, 500);
                        content.moveTo(60, 500); content.lineTo(460, 500);
                        content.moveTo(60, 550); content.lineTo(460, 550);
                        content.moveTo(60, 600); content.lineTo(460, 600);
                        content.stroke();
                    }
                }
            }
            document.save(target.toFile());
        }
    }

    private static String sha256(Path file) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }

    private record CorpusCase(String id, String token, boolean expectsOcr, boolean hybrid) {
    }

    private static final class CountingOcrEngine implements PdfOcrEngine {
        private final AtomicInteger totalCalls = new AtomicInteger();
        private final java.util.concurrent.ConcurrentHashMap<String, AtomicInteger> calls =
                new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public PdfOcrPageResult recognize(PdfOcrRequest request) {
            totalCalls.incrementAndGet();
            String id = request.sourcePdf().getFileName().toString()
                    .toLowerCase(Locale.ROOT).replaceAll("-á\\.pdf$", "");
            calls.computeIfAbsent(id, ignored -> new AtomicInteger()).incrementAndGet();
            String text = switch (id) {
                case "scanned" -> "ESCANEADO_OCR texto reconocido localmente sin perder líneas.";
                case "hybrid" -> "HIBRIDO_EVIDENCIA texto nativo dañado conserva evidencia contradictoria.";
                case "table" -> "TABLA_BUSCABLE Columna A Columna B Fila uno Valor dos.";
                case "formula" -> "FORMULA_BUSCABLE x igual a más b dividido para dos.";
                case "defective" -> "GALIMATIA_DUDOSA G4l1m at1a ?? texto dañado";
                default -> id.toUpperCase(Locale.ROOT) + " texto OCR.";
            };
            double confidence = id.equals("defective") ? 0.28 : 0.91;
            PdfPageRegion box = new PdfPageRegion(1, 60, 80, 540, 160, 612, 792);
            PdfOcrWord word = new PdfOcrWord(text, box, confidence);
            PdfOcrLine line = new PdfOcrLine(1, text, box, List.of(word), confidence);
            PdfTextLayer layer = new PdfTextLayer(1, PdfTextLayerOrigin.OCR_LOCAL,
                    List.of(line.toTextLine()), List.of());
            return new PdfOcrPageResult(1, 216, 1836, 2376, 612, 792,
                    List.of(line), List.of(word), layer, List.of());
        }

        int callsFor(String id) {
            return calls.getOrDefault(id, new AtomicInteger()).get();
        }
    }
}
