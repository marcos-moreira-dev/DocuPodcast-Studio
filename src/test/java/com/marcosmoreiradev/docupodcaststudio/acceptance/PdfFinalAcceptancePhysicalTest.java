package com.marcosmoreiradev.docupodcaststudio.acceptance;

import com.marcosmoreiradev.docupodcaststudio.application.document.*;
import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.application.script.BuildPreparedPdfNarrationUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.*;
import com.marcosmoreiradev.docupodcaststudio.localmedia.LocalMediaAdapters;
import com.marcosmoreiradev.docupodcaststudio.localmedia.LocalMediaLayout;
import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.apache.pdfbox.Loader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opt-in final physical acceptance matrix. It references legitimate local PDFs
 * in place and writes only audit evidence under target/.
 */
@EnabledIfSystemProperty(named = "docupodcast.pdf.finalAcceptance", matches = "true")
final class PdfFinalAcceptancePhysicalTest {
    private static final DateTimeFormatter RUN_FORMAT = DateTimeFormatter
            .ofPattern("uuuuMMdd-HHmmss").withZone(ZoneOffset.UTC);

    @Test
    void finalCorpusHasNoSilentlyWrongAcceptedPage() throws Exception {
        Path root = Path.of(System.getProperty(
                "docupodcast.pdf.finalAcceptance.root", "."))
                .toAbsolutePath().normalize();
        Path evidenceRoot = root.resolve("target/pdf-final-acceptance");
        Path run = Files.createDirectories(evidenceRoot.resolve(
                RUN_FORMAT.format(Instant.now())));
        Files.createDirectories(run.resolve("cases"));
        Files.writeString(evidenceRoot.resolve("latest-run.txt"),
                run.toString(), StandardCharsets.UTF_8);

        List<CorpusCase> corpus = corpus();
        String caseFilter = System.getProperty(
                "docupodcast.pdf.finalAcceptance.case", "").strip();
        if (!caseFilter.isBlank()) {
            corpus = corpus.stream().filter(sample ->
                    sample.id().equals(caseFilter)).toList();
            assertTrue(!corpus.isEmpty(), "Caso de corpus desconocido: " + caseFilter);
        }
        for (CorpusCase sample : corpus) {
            assertTrue(Files.isRegularFile(sample.source()),
                    "Falta corpus fisico: " + sample.source());
        }

        JsonPreparedPdfDocumentRepository repository =
                new JsonPreparedPdfDocumentRepository();
        JsonPdfOperationAttemptRepository attempts =
                new JsonPdfOperationAttemptRepository();
        PdfBoxNativeTextEvidenceExtractor nativeExtractor =
                new PdfBoxNativeTextEvidenceExtractor();
        PdfNativeTextQualityAssessor qualityAssessor =
                new PdfNativeTextQualityAssessor();
        LinkedHashMap<String, PreparedPdfWorkspaceRef> workspaces =
                initializeWorkspaces(run, corpus, repository);
        ArrayList<CaseResult> results = new ArrayList<>();

        boolean inventoryOnly = Boolean.getBoolean(
                "docupodcast.pdf.finalAcceptance.inventoryOnly");
        if (inventoryOnly) {
            for (CorpusCase sample : corpus) {
                PdfTextLayer nativeLayer = nativeExtractor.extract(
                        sample.source(), sample.page());
                PdfNativeTextQualityReport nativeQuality =
                        qualityAssessor.assess(nativeLayer);
                persistNativeEvidence(Files.createDirectories(
                        run.resolve("cases").resolve(sample.id())), nativeLayer);
                results.add(CaseResult.inventory(sample, nativeQuality));
            }
            writeReport(run, results, List.of());
            return;
        }

        ArrayList<String> defects = new ArrayList<>();
        PriorityResourceScheduler scheduler = new PriorityResourceScheduler(
                ComputeResourceBudget.incrementalReaderDefaults());
        try (MediaEnginePlatform physical = LocalMediaAdapters.create(
                LocalMediaLayout.development(root))) {
            ContentAnalysisEngine delegate = physical.contentAnalysisEngines()
                    .supporting(ContentAnalysisOperation.PAGE_SEMANTIC_READING)
                    .getFirst();
            RecordingContentEngine recorder = new RecordingContentEngine(delegate);
            ContentAnalysisEngineRegistry registry =
                    new ContentAnalysisEngineRegistry().register(recorder);
            MediaEnginePlatform instrumented = new MediaEnginePlatform(
                    physical.voiceEngines(), physical.imageEngines(),
                    physical.imageSuperResolutionEngines(),
                    physical.imageRefinementEngines(),
                    physical.videoGenerationEngines(),
                    physical.videoRenderEngines(), registry,
                    physical.administration());
            MediaCapabilityService media = new MediaCapabilityService(
                    instrumented, scheduler,
                    () -> ComputePreference.specificDevice(
                            "gpu-nvidia-0", true));
            AnalyzePdfPageSemanticallyUseCase reader =
                    new AnalyzePdfPageSemanticallyUseCase(
                            new PdfBoxRenderEngine(), media,
                            new BlockPdfSemanticPageResponseParser(), attempts,
                            nativeExtractor);

            for (CorpusCase sample : corpus) {
                System.out.println("FINAL_ACCEPTANCE_START=" + sample.id());
                Path caseEvidence = Files.createDirectories(
                        run.resolve("cases").resolve(sample.id()));
                recorder.beginCase(sample.id());
                long started = System.nanoTime();
                PdfTextLayer nativeLayer = nativeExtractor.extract(
                        sample.source(), sample.page());
                PdfNativeTextQualityReport nativeQuality =
                        qualityAssessor.assess(nativeLayer);
                persistNativeEvidence(caseEvidence, nativeLayer);
                PreparedPdfPage page = null;
                Throwable failure = null;
                try {
                    page = reader.analyze(workspaces.get(sample.documentId()),
                            sample.page(), null,
                            PdfPreparationCancellationToken.NONE,
                            PdfPreparationPriority.URGENT,
                            (stage, progress, message) -> appendEvent(
                                    caseEvidence, stage, progress, message));
                    repository.savePage(workspaces.get(sample.documentId())
                            .projectRoot(), page);
                } catch (Throwable problem) {
                    failure = problem;
                }
                long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
                List<CallRecord> calls = recorder.endCase();
                persistCalls(caseEvidence, calls);
                CaseResult result = assess(sample, nativeQuality, page, failure,
                        elapsedMs, calls, repository,
                        workspaces.get(sample.documentId()));
                results.add(result);
                defects.addAll(result.defects());
                persistCaseResult(caseEvidence, result);
                writeReport(run, results, defects);
                System.out.println("FINAL_ACCEPTANCE_RESULT=" + sample.id()
                        + "|" + result.finalResult() + "|calls=" + calls.size()
                        + "|defects=" + result.defects().size());
            }
        } finally {
            writeReport(run, results, defects);
        }

        verifyReopenAndHighlights(results, workspaces, repository, defects);
        writeReport(run, results, defects);
        assertTrue(defects.isEmpty(), () -> "Defectos del corpus final:\n- "
                + String.join("\n- ", defects));
    }

    private static CaseResult assess(
            CorpusCase sample,
            PdfNativeTextQualityReport nativeQuality,
            PreparedPdfPage page,
            Throwable failure,
            long elapsedMs,
            List<CallRecord> calls,
            JsonPreparedPdfDocumentRepository repository,
            PreparedPdfWorkspaceRef workspace) throws IOException {
        boolean accepted = page != null;
        boolean verifier = calls.stream().anyMatch(call ->
                call.pass().equals("coverage-verification"));
        long rois = calls.stream().filter(call ->
                call.pass().equals("targeted-recovery")).count();
        ArrayList<String> defects = new ArrayList<>();
        String text = accepted ? page.regions().stream()
                .map(PdfRegion::effectiveText)
                .collect(java.util.stream.Collectors.joining("\n")) : "";
        ArrayList<String> missing = new ArrayList<>();
        for (String anchor : sample.anchors()) {
            if (!normalized(text).contains(normalized(anchor))) missing.add(anchor);
        }
        boolean bboxValid = accepted && page.regions().stream().allMatch(region ->
                region.xMin() >= 0.0 && region.yMin() >= 0.0
                        && region.xMax() <= page.widthPoints()
                        && region.yMax() <= page.heightPoints()
                        && region.xMax() > region.xMin()
                        && region.yMax() > region.yMin());
        boolean readingOrderValid = accepted && readingOrderValid(page);
        Set<PdfRegionType> types = accepted ? page.regions().stream()
                .map(PdfRegion::effectiveType).collect(
                        java.util.stream.Collectors.toSet()) : Set.of();
        Set<PdfRegionType> missingTypes = new LinkedHashSet<>(sample.requiredTypes());
        missingTypes.removeAll(types);
        boolean speechValid = !sample.requiresVisualSpeech()
                || accepted && hasVisualSpeech(page);

        if (sample.expectReject()) {
            if (accepted) defects.add(sample.id()
                    + ": false accept de una pagina deliberadamente vacia");
        } else if (!accepted) {
            defects.add(sample.id() + ": false reject: "
                    + failureSummary(failure));
        }
        if (accepted && !missing.isEmpty()) {
            defects.add(sample.id() + ": omision silenciosa: " + missing);
        }
        if (accepted && !missingTypes.isEmpty()) {
            defects.add(sample.id() + ": tipos ausentes: " + missingTypes);
        }
        if (accepted && !bboxValid) {
            defects.add(sample.id() + ": bbox canonico invalido");
        }
        if (accepted && !readingOrderValid) {
            defects.add(sample.id() + ": readingOrder no determinista");
        }
        if (accepted && !speechValid) {
            defects.add(sample.id() + ": figura/grafica sin tratamiento narrativo");
        }
        if (accepted) {
            List<PdfRegion> unusablePlayback = page.regions().stream()
                    .filter(PdfRegion::playbackTarget)
                    .filter(region -> !region.container())
                    .filter(region -> switch (region.effectiveType()) {
                        case PARAGRAPH, SIDEBAR, LIST, UNKNOWN -> true;
                        default -> false;
                    })
                    .filter(region -> ((region.xMax() - region.xMin())
                            * (region.yMax() - region.yMin()))
                            / (page.widthPoints() * page.heightPoints()) >= 0.45)
                    .toList();
            if (!unusablePlayback.isEmpty()) {
                defects.add(sample.id() + ": UNUSABLE playback regions="
                        + unusablePlayback.stream().map(PdfRegion::id).toList());
            }
        }
        if (accepted && sample.expectedRole() != null
                && page.analysisProfile().effectiveRole() != sample.expectedRole()) {
            defects.add(sample.id() + ": pageRole esperado "
                    + sample.expectedRole() + " pero fue "
                    + page.analysisProfile().effectiveRole());
        }
        if (accepted && !sample.orderedAnchors().isEmpty()
                && !ordered(text, sample.orderedAnchors())) {
            defects.add(sample.id() + ": columnas mezcladas: "
                    + sample.orderedAnchors());
        }
        if (accepted && "demo-p2".equals(sample.id())) {
            verifyDemoPageTwoTitleAndRunningHeader(page, calls, defects);
        }

        int segments = 0;
        int bindings = 0;
        if (accepted) {
            var script = new BuildPreparedPdfNarrationUseCase(repository)
                    .buildPage(workspace, sample.page(), sample.id(),
                            page.analysisProfile().language().effectiveLanguage(),
                            false, null, null);
            segments = script.segments().size();
            bindings = (int) script.segments().stream().filter(segment ->
                    PdfNarrationBindingMetadata.decode(segment.metadata()
                            .get(PdfNarrationBindingMetadata.KEY)).isPresent()).count();
            if (!sample.expectNoNarration() && segments == 0) {
                defects.add(sample.id() + ": pagina aceptada sin NarrationSegment");
            }
            if (bindings != segments) {
                defects.add(sample.id() + ": NarrationSegment sin binding PDF");
            }
        }

        long promptTokens = calls.stream().mapToLong(CallRecord::promptTokens).sum();
        long outputTokens = calls.stream().mapToLong(CallRecord::outputTokens).sum();
        double minTps = calls.stream().mapToDouble(CallRecord::tokensPerSecond)
                .filter(value -> value > 0.0).min().orElse(0.0);
        double maxTps = calls.stream().mapToDouble(CallRecord::tokensPerSecond)
                .filter(value -> value > 0.0).max().orElse(0.0);
        String finalResult = accepted ? "ACCEPTED" : "REJECTED";
        return new CaseResult(sample, nativeQuality.quality().name(),
                nativeQuality.score(), calls.isEmpty() ? "NOT_RUN"
                : calls.getFirst().success() ? "VALID" : "FAILED",
                verifier, rois > 0, (int) rois, false, finalResult,
                accepted ? page.regions().size() : 0,
                accepted ? coverage(page) : "NOT_PUBLISHED",
                bboxValid, readingOrderValid, missing.isEmpty(), elapsedMs,
                promptTokens, outputTokens, minTps, maxTps,
                segments, bindings, failureSummary(failure),
                List.copyOf(defects));
    }

    private static void verifyDemoPageTwoTitleAndRunningHeader(
            PreparedPdfPage page, List<CallRecord> calls, List<String> defects) {
        String expectedTitle = normalized(
                "La demostracion: encerrar lo desconocido entre dos certezas");
        String runningHeader = normalized(
                "Un limite notable, visto de cerca Calculo diferencial");
        PdfRegion title = page.regions().stream()
                .filter(region -> normalized(region.effectiveText())
                        .contains(expectedTitle))
                .findFirst().orElse(null);
        if (title == null) {
            defects.add("demo-p2: titulo academico principal ausente");
        } else {
            if (!Set.of(PdfRegionType.TITLE, PdfRegionType.HEADING,
                            PdfRegionType.SUBHEADING)
                    .contains(title.effectiveType())) {
                defects.add("demo-p2: titulo clasificado como "
                        + title.effectiveType());
            }
            if (!title.playbackTarget()
                    || title.effectiveNarratability()
                    == PdfNarratability.NON_NARRATABLE) {
                defects.add("demo-p2: titulo academico no narrable");
            }
            int nextBodyOrder = page.regions().stream()
                    .filter(region -> region.playbackTarget())
                    .filter(region -> region.effectiveType()
                            == PdfRegionType.PARAGRAPH)
                    .mapToInt(PdfRegion::readingOrder).min()
                    .orElse(Integer.MAX_VALUE);
            if (title.readingOrder() >= nextBodyOrder) {
                defects.add("demo-p2: titulo fuera del orden fisico de lectura");
            }
        }
        List<PdfRegion> headers = page.regions().stream()
                .filter(region -> normalized(region.effectiveText())
                        .contains(runningHeader)
                        || runningHeader.contains(normalized(region.effectiveText())))
                .toList();
        if (headers.isEmpty()) {
            defects.add("demo-p2: no se audito el encabezado repetido");
        } else if (headers.stream().anyMatch(region ->
                region.effectiveType() != PdfRegionType.HEADER
                        || region.playbackTarget()
                        || region.effectiveNarratability()
                        != PdfNarratability.NON_NARRATABLE)) {
            defects.add("demo-p2: encabezado repetido sigue narrable o mal tipado");
        }
        long titleOccurrences = page.regions().stream()
                .filter(region -> normalized(region.effectiveText())
                        .contains(expectedTitle)).count();
        if (titleOccurrences != 1) {
            defects.add("demo-p2: titulo duplicado count=" + titleOccurrences);
        }
        if (calls.stream().noneMatch(call ->
                call.pass().equals("targeted-recovery"))) {
            defects.add("demo-p2: faltan evidencia y llamada de recovery focal");
        }
    }

    private static void verifyReopenAndHighlights(
            List<CaseResult> results,
            Map<String, PreparedPdfWorkspaceRef> workspaces,
            JsonPreparedPdfDocumentRepository repository,
            List<String> defects) throws IOException {
        JsonPreparedPdfDocumentRepository reopened =
                new JsonPreparedPdfDocumentRepository();
        for (var entry : workspaces.entrySet()) {
            PreparedPdfWorkspaceRef workspace = entry.getValue();
            List<PreparedPdfPage> pages = reopened.loadPages(workspace.projectRoot());
            Set<Integer> expected = results.stream()
                    .filter(result -> result.sample().documentId().equals(entry.getKey()))
                    .filter(result -> result.finalResult().equals("ACCEPTED"))
                    .map(result -> result.sample().page())
                    .collect(java.util.stream.Collectors.toSet());
            Set<Integer> actual = pages.stream().map(PreparedPdfPage::pageNumber)
                    .collect(java.util.stream.Collectors.toSet());
            if (!actual.containsAll(expected)) {
                defects.add(entry.getKey() + ": reopen perdio paginas " + expected);
            }
            PdfVisualReadingProjection projection =
                    new BuildPdfVisualReadingProjectionUseCase(reopened)
                            .build(workspace);
            for (PreparedPdfPage page : pages) {
                for (PdfRegion region : page.regions()) {
                    if (region.playbackTarget()
                            && projection.highlightForRegion(region.id()).isEmpty()) {
                        defects.add(entry.getKey() + ": highlight ausente para "
                                + region.id());
                    }
                    if (!region.playbackTarget()
                            && projection.highlightForRegion(region.id()).isPresent()) {
                        defects.add(entry.getKey() + ": contenedor con highlight "
                                + region.id());
                    }
                }
            }
            BuildPreparedPdfNarrationUseCase narration =
                    new BuildPreparedPdfNarrationUseCase(reopened);
            for (int pageNumber : expected) {
                var first = narration.buildPage(workspace, pageNumber,
                        entry.getKey(), "und", false, null, null);
                var second = narration.buildPage(workspace, pageNumber,
                        entry.getKey(), "und", false, null, null);
                if (!first.segments().stream().map(segment -> segment.id()).toList()
                        .equals(second.segments().stream().map(segment -> segment.id()).toList())) {
                    defects.add(entry.getKey() + ": reopen cambio IDs de narracion P"
                            + pageNumber);
                }
            }
        }
    }

    private static LinkedHashMap<String, PreparedPdfWorkspaceRef> initializeWorkspaces(
            Path run, List<CorpusCase> corpus,
            JsonPreparedPdfDocumentRepository repository) throws Exception {
        LinkedHashMap<String, PreparedPdfWorkspaceRef> result = new LinkedHashMap<>();
        for (CorpusCase sample : corpus) {
            if (result.containsKey(sample.documentId())) continue;
            Path projectRoot = Files.createDirectories(
                    run.resolve("workspaces").resolve(sample.documentId()));
            String hash = sha256(sample.source());
            int pages;
            try (var document = Loader.loadPDF(sample.source().toFile())) {
                pages = document.getNumberOfPages();
            }
            repository.initialize(projectRoot, new PdfDocumentManifest(
                    PdfDocumentManifest.CURRENT_SCHEMA_VERSION,
                    sample.documentId(), sample.source().getFileName().toString(),
                    hash, pages, "pdf-final-acceptance-v1",
                    Instant.now(), Instant.now()));
            result.put(sample.documentId(), new PreparedPdfWorkspaceRef(
                    projectRoot, sample.source(), hash));
        }
        return result;
    }

    private static List<CorpusCase> corpus() {
        Path demo = corpusFile("Demostracion_limite_notable.pdf",
                "D:/Proyectos/Demostracion_limite_notable.pdf");
        Path informe = corpusFile("informe.pdf",
                "D:/Proyectos/g/tmp/docx_review/informe.pdf");
        Path edsac = corpusFile("EdsacDoc.pdf",
                "C:/Users/MARCOS MOREIRA/Documents/Edsac Simulator/Resources/EdsacDoc.pdf");
        Path edsacGuide = corpusFile("EdsacTG.pdf",
                "C:/Users/MARCOS MOREIRA/Documents/Edsac Simulator/Resources/EdsacTG.pdf");
        Path electronics = corpusFile("Manual de Electrónica Básica Montecarlo 1.1.pdf",
                "C:/Users/MARCOS MOREIRA/Documents/MEGA/Mis cosas personales/Manual de Electrónica Básica Montecarlo 1.1.pdf");
        Path kress = corpusFile("Numerical Analysis - Rainer Kress.pdf",
                "C:/Users/MARCOS MOREIRA/Desktop/Numerical Analysis - Rainer Kress.pdf");
        Path burden = corpusFile("Numerical Analysis NINTH EDITION Richard L. Burden.pdf",
                "C:/Users/MARCOS MOREIRA/Desktop/Numerical Analysis NINTH EDITION Richard L. Burden.pdf");
        Path wwg = corpusFile("WWG1951.pdf",
                "C:/Users/MARCOS MOREIRA/Documents/Edsac Simulator/Resources/WWG1951.pdf");
        return List.of(
                sample("demo-p1", "demo", demo, 1,
                        "prosa+matematica+figura", false,
                        List.of("fracción indeterminada", "radianes", "sector circular"),
                        Set.of(PdfRegionType.IMAGE), true, null, List.of()),
                sample("demo-p2", "demo", demo, 2,
                        "tabla-simple+matematica", false,
                        List.of("Triángulo interior OAB", "½ sin x", "Resultado final"),
                        Set.of(PdfRegionType.TABLE), false,
                        null, List.of()),
                sample("demo-p3", "demo", demo, 3,
                        "grafica+matematica+prosa", false,
                        List.of("La intuición visual", "sin x ≈ x", "Lectura conceptual"),
                        Set.of(PdfRegionType.IMAGE), true,
                        null, List.of()),
                sample("informe-p2", "informe", informe, 2,
                        "tabla-densa", true,
                        List.of("Hallazgos en una página", "4 GB de VRAM", "3.2 GB compartidos"),
                        Set.of(PdfRegionType.TABLE), false, null, List.of()),
                sample("edsac-p10", "edsac", edsac, 10,
                        "codigo+multicolumna", true,
                        List.of("dummy print routine", "number of digits", "closed sub-routines"),
                        Set.of(), false, null, List.of()),
                sample("edsactg-p10", "edsactg", edsacGuide, 10,
                        "prosa-tecnica", false,
                        List.of("The Simulator Environment", "integral text editor", "keystroke equivalent"),
                        Set.of(), false, null, List.of()),
                sample("electronics-p10", "electronics", electronics, 10,
                        "prosa+formulas", false,
                        List.of("la potencia", "P = V * I", "V = I / P"),
                        Set.of(PdfRegionType.MATH), false, null, List.of()),
                sample("kress-p20", "kress", kress, 20,
                        "matematica+prosa", true,
                        List.of("tridiagonal matrix", "Example 2.2", "Dirichlet boundary condition"),
                        Set.of(PdfRegionType.MATH), false, null, List.of()),
                new CorpusCase("kress-p335", "kress", kress, 335,
                        "indice-multicolumna", true,
                        List.of("Index", "Adams-Bashforth", "convergence order"),
                        Set.of(), false, PdfPageRole.INDEX,
                        List.of("Adams-Bashforth", "Cauchy sequence",
                                "Cauchy-Schwarz", "convergent quadrature"), true),
                sample("burden-p20", "burden", burden, 20,
                        "grafica+matematica+prosa", true,
                        List.of("Review of Calculus", "Definition 1.1", "Limits and Continuity"),
                        Set.of(PdfRegionType.IMAGE, PdfRegionType.MATH), true,
                        null, List.of()),
                sample("kress-p1", "kress", kress, 1,
                        "rasterizada-sin-native-text-fiable", true,
                        List.of("Graduate Texts in Mathematics", "Numerical Analysis", "Rainer Kress"),
                        Set.of(), false, null, List.of()),
                new CorpusCase("wwg-p10", "wwg", wwg, 10,
                        "escaneada-casi-vacia", true, List.of(), Set.of(),
                        false, null, List.of(), true));
    }

    private static Path corpusFile(String fileName, String localDefault) {
        String configuredDirectory = System.getProperty(
                "docupodcast.pdf.finalAcceptance.corpusDir", "").trim();
        return configuredDirectory.isEmpty()
                ? Path.of(localDefault)
                : Path.of(configuredDirectory).resolve(fileName);
    }

    private static CorpusCase sample(
            String id, String documentId, Path source, int page,
            String category, boolean expectReject, List<String> anchors,
            Set<PdfRegionType> types, boolean speech, PdfPageRole role,
            List<String> orderedAnchors) {
        return new CorpusCase(id, documentId, source, page, category,
                expectReject, anchors, types, speech, role, orderedAnchors,
                role == PdfPageRole.INDEX || expectReject);
    }

    private static boolean readingOrderValid(PreparedPdfPage page) {
        List<Integer> order = page.regions().stream()
                .map(PdfRegion::readingOrder).toList();
        for (int index = 0; index < order.size(); index++) {
            if (order.get(index) != index) return false;
        }
        return true;
    }

    private static boolean hasVisualSpeech(PreparedPdfPage page) {
        Set<String> visualIds = page.regions().stream()
                .filter(region -> region.effectiveType() == PdfRegionType.IMAGE)
                .map(PdfRegion::id).collect(java.util.stream.Collectors.toSet());
        return !visualIds.isEmpty() && page.derivedTreatments().stream()
                .filter(treatment -> treatment.kind()
                        == PdfDerivedTreatmentKind.IMAGE_DESCRIPTION)
                .anyMatch(treatment -> treatment.sourceRegionIds().stream()
                        .anyMatch(visualIds::contains)
                        && !treatment.derivedText().isBlank());
    }

    private static String coverage(PreparedPdfPage page) {
        return page.regions().stream().map(region -> region.attributes()
                        .getOrDefault("semanticCoverageStatus", "UNKNOWN"))
                .distinct().collect(java.util.stream.Collectors.joining(","));
    }

    private static boolean ordered(String text, List<String> anchors) {
        String value = normalized(text);
        int previous = -1;
        for (String anchor : anchors) {
            int current = value.indexOf(normalized(anchor));
            if (current < 0 || current <= previous) return false;
            previous = current;
        }
        return true;
    }

    private static String normalized(String value) {
        return Normalizer.normalize(Objects.toString(value, ""),
                        Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
                .replace('−', '-').replace('–', '-').replace('—', '-')
                .replaceAll("[^\\p{L}\\p{N}/*+=<>.%-]+", " ")
                .replaceAll("\\s+", " ").strip();
    }

    private static String failureSummary(Throwable failure) {
        if (failure == null) return "";
        return failure.getClass().getSimpleName() + ": "
                + Objects.toString(failure.getMessage(), "");
    }

    private static void appendEvent(Path directory, String stage,
                                    double progress, String message) {
        try {
            Files.writeString(directory.resolve("events.log"),
                    Instant.now() + "|" + stage + "|" + progress + "|"
                            + message + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    private static void persistCalls(Path directory, List<CallRecord> calls)
            throws IOException {
        int index = 0;
        for (CallRecord call : calls) {
            index++;
            String prefix = "%02d-%s".formatted(index, call.pass());
            Files.writeString(directory.resolve(prefix + ".raw.txt"),
                    call.raw(), StandardCharsets.UTF_8);
            Files.writeString(directory.resolve(prefix + ".metrics.txt"),
                    "success=" + call.success() + "\n"
                            + "durationMs=" + call.durationMs() + "\n"
                            + "promptTokens=" + call.promptTokens() + "\n"
                            + "outputTokens=" + call.outputTokens() + "\n"
                            + "tokensPerSecond=" + call.tokensPerSecond() + "\n"
                            + "failure=" + call.failure() + "\n",
                    StandardCharsets.UTF_8);
        }
    }

    private static void persistNativeEvidence(Path directory, PdfTextLayer layer)
            throws IOException {
        StringBuilder output = new StringBuilder();
        for (PdfTextLine line : layer.lines()) {
            PdfPageRegion box = line.region();
            output.append(box.xMinPoints()).append('|')
                    .append(box.yMinPoints()).append('|')
                    .append(box.xMaxPoints()).append('|')
                    .append(box.yMaxPoints()).append('|')
                    .append(line.text().replace("\r", " ").replace("\n", " "))
                    .append(System.lineSeparator());
        }
        Files.writeString(directory.resolve("native-evidence.txt"), output,
                StandardCharsets.UTF_8);
    }

    private static void persistCaseResult(Path directory, CaseResult result)
            throws IOException {
        Properties values = new Properties();
        values.setProperty("sample", result.sample().id());
        values.setProperty("document", result.sample().documentId());
        values.setProperty("page", Integer.toString(result.sample().page()));
        values.setProperty("category", result.sample().category());
        values.setProperty("nativeQuality", result.nativeQuality());
        values.setProperty("nativeScore", Double.toString(result.nativeScore()));
        values.setProperty("primaryResult", result.primaryResult());
        values.setProperty("verifierUsed", Boolean.toString(result.verifierUsed()));
        values.setProperty("recoveryUsed", Boolean.toString(result.recoveryUsed()));
        values.setProperty("rois", Integer.toString(result.rois()));
        values.setProperty("fallbackUsed", Boolean.toString(result.fallbackUsed()));
        values.setProperty("finalResult", result.finalResult());
        values.setProperty("regions", Integer.toString(result.regions()));
        values.setProperty("coverage", result.coverage());
        values.setProperty("bboxValid", Boolean.toString(result.bboxValid()));
        values.setProperty("readingOrderValid",
                Boolean.toString(result.readingOrderValid()));
        values.setProperty("contentPreserved",
                Boolean.toString(result.contentPreserved()));
        values.setProperty("elapsedMs", Long.toString(result.elapsedMs()));
        values.setProperty("promptTokens", Long.toString(result.promptTokens()));
        values.setProperty("outputTokens", Long.toString(result.outputTokens()));
        values.setProperty("minTokensPerSecond",
                Double.toString(result.minTokensPerSecond()));
        values.setProperty("maxTokensPerSecond",
                Double.toString(result.maxTokensPerSecond()));
        values.setProperty("narrationSegments",
                Integer.toString(result.narrationSegments()));
        values.setProperty("narrationBindings",
                Integer.toString(result.narrationBindings()));
        values.setProperty("failure", result.failure());
        values.setProperty("defects", String.join(" || ", result.defects()));
        try (var output = Files.newOutputStream(
                directory.resolve("case-result.properties"))) {
            values.store(output, "DocuPodcast PDF final physical acceptance");
        }
    }

    private static void writeReport(Path run, List<CaseResult> results,
                                    List<String> defects) throws IOException {
        int accepted = (int) results.stream().filter(result ->
                result.finalResult().equals("ACCEPTED")).count();
        int rejected = (int) results.stream().filter(result ->
                result.finalResult().equals("REJECTED")).count();
        int primaryOnly = (int) results.stream().filter(result ->
                result.finalResult().equals("ACCEPTED")
                        && !result.verifierUsed()).count();
        int verifier = (int) results.stream().filter(result ->
                result.finalResult().equals("ACCEPTED")
                        && result.verifierUsed() && !result.recoveryUsed()).count();
        int recovery = (int) results.stream().filter(result ->
                result.finalResult().equals("ACCEPTED")
                        && result.recoveryUsed()).count();
        StringBuilder markdown = new StringBuilder("""
                # PDF final robustness acceptance

                - generated: %s
                - samples completed: %d
                - accepted: %d
                - primary-only accepted: %d
                - accepted after verifier: %d
                - accepted after recovery: %d
                - rejected/fallback: %d
                - silent omissions/defects: %d

                | Sample | Category | Native | Primary | Verifier | Recovery/ROIs | Final | Regions | Coverage | BBox | Order | Preserved | Time ms | Tokens | tok/s |
                |---|---|---:|---|---:|---:|---|---:|---|---:|---:|---:|---:|---:|---:|
                """.formatted(Instant.now(), results.size(), accepted,
                primaryOnly, verifier, recovery, rejected, defects.size()));
        for (CaseResult result : results) {
            markdown.append("| ").append(result.sample().id()).append(" | ")
                    .append(result.sample().category()).append(" | ")
                    .append(result.nativeQuality()).append(" | ")
                    .append(result.primaryResult()).append(" | ")
                    .append(result.verifierUsed()).append(" | ")
                    .append(result.recoveryUsed()).append('/').append(result.rois())
                    .append(" | ").append(result.finalResult()).append(" | ")
                    .append(result.regions()).append(" | ")
                    .append(result.coverage()).append(" | ")
                    .append(result.bboxValid()).append(" | ")
                    .append(result.readingOrderValid()).append(" | ")
                    .append(result.contentPreserved()).append(" | ")
                    .append(result.elapsedMs()).append(" | ")
                    .append(result.promptTokens()).append('+')
                    .append(result.outputTokens()).append(" | ")
                    .append(String.format(Locale.ROOT, "%.3f..%.3f",
                            result.minTokensPerSecond(), result.maxTokensPerSecond()))
                    .append(" |\n");
        }
        if (!defects.isEmpty()) {
            markdown.append("\n## Defects\n\n");
            defects.forEach(value -> markdown.append("- ").append(value)
                    .append("\n"));
        }
        Files.writeString(run.resolve("PDF_FINAL_ACCEPTANCE_REPORT.md"),
                markdown, StandardCharsets.UTF_8);
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(path)) {
            byte[] buffer = new byte[256 * 1024];
            for (int read; (read = input.read(buffer)) >= 0; ) {
                if (read > 0) digest.update(buffer, 0, read);
            }
        }
        return java.util.HexFormat.of().formatHex(digest.digest());
    }

    private record CorpusCase(
            String id, String documentId, Path source, int page,
            String category, boolean expectReject, List<String> anchors,
            Set<PdfRegionType> requiredTypes, boolean requiresVisualSpeech,
            PdfPageRole expectedRole, List<String> orderedAnchors,
            boolean expectNoNarration) {
    }

    private record CaseResult(
            CorpusCase sample, String nativeQuality, double nativeScore,
            String primaryResult, boolean verifierUsed, boolean recoveryUsed,
            int rois, boolean fallbackUsed, String finalResult, int regions,
            String coverage, boolean bboxValid, boolean readingOrderValid,
            boolean contentPreserved, long elapsedMs, long promptTokens,
            long outputTokens, double minTokensPerSecond,
            double maxTokensPerSecond, int narrationSegments,
            int narrationBindings, String failure, List<String> defects) {
        private static CaseResult inventory(CorpusCase sample,
                                            PdfNativeTextQualityReport report) {
            return new CaseResult(sample, report.quality().name(), report.score(),
                    "NOT_RUN", false, false, 0, false, "INVENTORY", 0,
                    "NOT_RUN", false, false, false, 0L, 0L, 0L,
                    0.0, 0.0, 0, 0, "", List.of());
        }
    }

    private record CallRecord(
            String pass, boolean success, String raw, long durationMs,
            long promptTokens, long outputTokens, double tokensPerSecond,
            String failure) {
    }

    private static final class RecordingContentEngine
            implements ContentAnalysisEngine, ContentAnalysisBatchLifecycle {
        private final ContentAnalysisEngine delegate;
        private final List<CallRecord> calls = new CopyOnWriteArrayList<>();
        private volatile String caseId = "";

        private RecordingContentEngine(ContentAnalysisEngine delegate) {
            this.delegate = Objects.requireNonNull(delegate);
        }

        private void beginCase(String value) {
            caseId = value;
            calls.clear();
        }

        private List<CallRecord> endCase() {
            return List.copyOf(calls);
        }

        @Override
        public Set<ContentAnalysisOperation> operations() {
            return delegate.operations();
        }

        @Override
        public ContentAnalysisResult analyze(ContentAnalysisRequest request,
                                             ExecutionContext context)
                throws IOException, InterruptedException {
            String pass = request.options().getOrDefault("semanticPass", "primary");
            long started = System.nanoTime();
            try {
                ContentAnalysisResult result = delegate.analyze(request, context);
                calls.add(record(pass, true, result.structuredJson(),
                        result.diagnostics(), started, ""));
                return result;
            } catch (IOException | InterruptedException | RuntimeException failure) {
                calls.add(record(pass, false, "", Map.of(), started,
                        caseId + ": " + failure));
                throw failure;
            }
        }

        private static CallRecord record(
                String pass, boolean success, String raw,
                Map<String, String> diagnostics, long started, String failure) {
            return new CallRecord(pass, success, Objects.toString(raw, ""),
                    number(diagnostics, "durationMs",
                            (System.nanoTime() - started) / 1_000_000L),
                    number(diagnostics, "promptTokens", 0L),
                    number(diagnostics, "outputTokens", 0L),
                    decimal(diagnostics, "generationTokensPerSecond"), failure);
        }

        @Override
        public EngineDescriptor descriptor() {
            return delegate.descriptor();
        }

        @Override
        public EngineConfigurationSchema configurationSchema() {
            return delegate.configurationSchema();
        }

        @Override
        public EngineReadiness inspectReadiness(EngineConfiguration configuration) {
            return delegate.inspectReadiness(configuration);
        }

        @Override
        public List<EnginePresetDescriptor> presets() {
            return delegate.presets();
        }

        @Override
        public Object contentAnalysisBatchIdentity() {
            return delegate instanceof ContentAnalysisBatchLifecycle lifecycle
                    ? lifecycle.contentAnalysisBatchIdentity() : delegate;
        }

        @Override
        public void beginContentAnalysisBatch(ExecutionContext context)
                throws IOException, InterruptedException {
            if (delegate instanceof ContentAnalysisBatchLifecycle lifecycle) {
                lifecycle.beginContentAnalysisBatch(context);
            }
        }

        @Override
        public void endContentAnalysisBatch(ExecutionContext context)
                throws IOException, InterruptedException {
            if (delegate instanceof ContentAnalysisBatchLifecycle lifecycle) {
                lifecycle.endContentAnalysisBatch(context);
            }
        }

        private static long number(Map<String, String> values, String key,
                                   long fallback) {
            try {
                return Long.parseLong(values.getOrDefault(key,
                        Long.toString(fallback)));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }

        private static double decimal(Map<String, String> values, String key) {
            try {
                return Double.parseDouble(values.getOrDefault(key, "0"));
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }
    }
}
