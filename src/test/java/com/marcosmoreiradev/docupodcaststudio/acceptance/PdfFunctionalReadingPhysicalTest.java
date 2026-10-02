package com.marcosmoreiradev.docupodcaststudio.acceptance;

import com.marcosmoreiradev.docupodcaststudio.application.document.*;
import com.marcosmoreiradev.docupodcaststudio.application.script.BuildPreparedPdfNarrationUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.script.ValidatePreparedPdfNarrationCoverageUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.script.PdfNarrationCoverageStatus;
import com.marcosmoreiradev.docupodcaststudio.application.reading.AdaptNarrationLanguageUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.reading.NarrationLanguageDetector;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingInterval;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingIntervalUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingScope;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticReadingPolicy;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPreparedPdfDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Read-only physical gate over already-published canonical pages; it never invokes Qwen. */
@EnabledIfSystemProperty(named = "docupodcast.pdf.functionalReading", matches = "true")
final class PdfFunctionalReadingPhysicalTest {

    @Test
    void canonicalDemoFlowsThroughNarrationHighlightAndAudiovisualProjection()
            throws Exception {
        Path project = Path.of(System.getProperty("docupodcast.pdf.functionalReading.project",
                "D:/Proyectos/Demostracion_limite_notable")).toAbsolutePath().normalize();
        Path source = project.resolve("source/Demostracion_limite_notable.pdf");
        var repository = new JsonPreparedPdfDocumentRepository();
        var manifest = repository.loadManifest(project).orElseThrow();
        var workspace = new PreparedPdfWorkspaceRef(project, source,
                manifest.sourceSha256());
        Map<Path, String> before = canonicalHashes(project);

        var script = new BuildPreparedPdfNarrationUseCase(repository).build(
                workspace, manifest.title(), "es", false, null,
                SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF);
        assertFalse(script.empty());
        assertEquals(List.of(1, 2, 3), script.segments().stream()
                .map(segment -> Integer.parseInt(segment.metadata().get("pdfSourcePage")))
                .distinct().toList());
        assertPageTypes(script, 2, "PARAGRAPH", "TABLE");
        assertPageTypes(script, 3, "PARAGRAPH", "IMAGE", "SIDEBAR");
        var interval = new ResolveDocumentProcessingSelectionUseCase().execute(
                script, DocumentProcessingScope.INTERVAL,
                new DocumentProcessingInterval(DocumentProcessingIntervalUnit.PAGE, 2, 3), "",
                source.toString(), manifest.sourceSha256());
        assertEquals(List.of(2, 3), interval.resolvedPageNumbers());
        assertEquals(17, interval.resolvedSegmentIds().size());
        assertTrue(interval.narration().segments().stream().allMatch(segment -> {
            int page = Integer.parseInt(segment.metadata().get("pdfSourcePage"));
            return page == 2 || page == 3;
        }));
        var pages = repository.loadPages(project);
        var coverage = new ValidatePreparedPdfNarrationCoverageUseCase().validate(
                pages, script, SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF);
        assertTrue(coverage.complete(), () -> "Cobertura incompleta: " + coverage.incompleteItems());
        var resultRegion = pages.stream().flatMap(page -> page.regions().stream())
                .filter(region -> region.effectiveText().contains("Resultado final"))
                .findFirst().orElseThrow();
        var resultCoverage = coverage.items().stream()
                .filter(item -> item.regionId().equals(resultRegion.id())).findFirst().orElseThrow();
        assertEquals(PdfNarrationCoverageStatus.NARRATED, resultCoverage.status());
        assertEquals(2, resultRegion.pageNumber());
        var resultSegment = script.segments().stream()
                .filter(segment -> segment.sourceBlockIds().contains(resultRegion.id()))
                .findFirst().orElseThrow();
        assertFalse(resultSegment.narrationText().contains("?"), resultSegment.narrationText());
        for (var segment : script.segments()) {
            assertFalse(segment.narrationText().contains("$"), segment.narrationText());
            assertFalse(segment.narrationText().matches("(?s).*\\\\(?:frac|sin|cos|tan|lim).*"),
                    segment.narrationText());
        }

        var visual = new BuildPdfVisualReadingProjectionUseCase(repository).build(workspace);
        var highlightResolver = new ResolvePdfPlaybackHighlightUseCase();
        var visualGuard = new PdfPlaybackVisualGuard();
        var transitions = new java.util.ArrayList<String>();
        String previousRegionId = "";
        long sequence = 0;
        for (var segment : script.segments()) {
            var binding = PdfNarrationBindingMetadata.decode(segment.metadata().get(
                    PdfNarrationBindingMetadata.KEY)).orElseThrow();
            String regionId = segment.metadata().get("sourceBlockId");
            assertNotNull(regionId, segment.id());
            var target = highlightResolver.resolveTarget(
                    visual, segment, previousRegionId, segment.id() + "-U001")
                    .orElseThrow(() -> new AssertionError("Sin target para " + segment.id()));
            assertEquals(regionId, target.regionId(),
                    () -> segment.id() + " resaltó " + target.regionId()
                            + " en lugar de " + regionId);
            assertEquals(binding.pageNumber(), target.pageNumber());
            sequence++;
            assertTrue(visualGuard.request(sequence, regionId));
            assertTrue(visualGuard.painted(sequence, target.regionId()));
            var canonicalRegion = pages.stream().flatMap(page -> page.regions().stream())
                    .filter(candidate -> candidate.id().equals(regionId))
                    .findFirst().orElseThrow();
            transitions.add(String.join("|", Long.toString(sequence), segment.id(),
                    Integer.toString(binding.pageNumber()), regionId,
                    Integer.toString(canonicalRegion.effectiveReadingOrder()),
                    target.regionId(), bbox(target.region()), classification(visual, target)));
            previousRegionId = regionId;
        }

        var projection = new BuildDocumentContentProjectionUseCase(
                new OpenPreparedPdfWorkspaceUseCase(repository)).build(
                new PreparedPdfSource(workspace, manifest.title()), script);
        assertFalse(projection.items().isEmpty());
        assertTrue(projection.items().stream().anyMatch(item -> item.kind().name().equals("TABLE")));
        assertTrue(projection.items().stream().anyMatch(item -> item.kind().name().equals("IMAGE")));
        assertTrue(projection.items().stream().allMatch(item -> item.pdfAnchor().isPresent()));

        assertEquals(before, canonicalHashes(project), "El gate físico modificó páginas canónicas");
        Path report = Path.of("target/pdf-functional-reading/physical-gate.txt")
                .toAbsolutePath().normalize();
        Files.createDirectories(report.getParent());
        String types = script.segments().stream().collect(Collectors.groupingBy(
                segment -> segment.metadata().get("sourceBlockType"),
                LinkedHashMap::new, Collectors.counting())).toString();
        String matrix = coverage.items().stream().map(item -> String.join("|",
                        Integer.toString(item.pageNumber()), Integer.toString(item.readingOrder()),
                        item.regionId(), item.regionType(), item.status().name(),
                        String.join(",", item.narrationSegmentIds()), item.reason()))
                .collect(Collectors.joining("\n"));
        Properties translations = new Properties();
        Path translationFile = project.resolve("derived/narration-translation-v1.properties");
        if (Files.isRegularFile(translationFile)) {
            try (var input = Files.newInputStream(translationFile)) {
                translations.load(input);
            }
        }
        long fullDocumentTranslationCacheHits = script.segments().stream()
                .map(segment -> AdaptNarrationLanguageUseCase.fingerprint(
                        segment.narrationText(),
                        segment.metadata().getOrDefault("sourceLanguage", "es"), "en"))
                .filter(translations::containsKey).count();
        NarrationLanguageDetector detector = new NarrationLanguageDetector();
        java.util.Set<String> currentScopeIds = java.util.Set.copyOf(
                interval.resolvedSegmentIds());
        String derivativeMatrix = script.segments().stream().map(segment -> {
            String sourceLanguage = detector.detect(segment.narrationText(),
                    segment.metadata().getOrDefault("sourceLanguage", "es"));
            String fingerprint = AdaptNarrationLanguageUseCase.fingerprint(
                    segment.narrationText(), sourceLanguage, "en");
            String translated = translations.getProperty(fingerprint, "").strip();
            boolean validTranslation = !translated.isBlank()
                    && (detector.confidentlyMatches(translated, "en")
                    || detector.languageNeutralMathematicalNotation(translated));
            String translationStatus = translated.isBlank()
                    ? "TRANSLATION_MISSING"
                    : validTranslation ? "TRANSLATION_VALID" : "TRANSLATION_INVALID";
            String effectiveText = validTranslation ? translated : "";
            String expectedAudio = validTranslation
                    ? AudioGenerationUnit.fromSegment(new com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment(
                    segment.id(), segment.type(), segment.title(), translated,
                    segment.sourceBlockIds(), segment.characterId(), segment.voiceProfileId(),
                    segment.performanceStyleId(), segment.metadata()))
                    .sourceFingerprint().toString()
                    : "UNRESOLVED_TRANSLATION";
            return String.join("|", segment.id(),
                    segment.metadata().getOrDefault("sourceBlockId",
                            segment.sourceBlockIds().isEmpty() ? "" : segment.sourceBlockIds().getFirst()),
                    segment.metadata().getOrDefault("pdfSourcePage", ""),
                    hash(segment.narrationText()), sourceLanguage, "en",
                    translated.isBlank() ? "MISS" : "HIT", fingerprint,
                    translationStatus, Boolean.toString(validTranslation),
                    validTranslation ? hash(effectiveText) : "",
                    validTranslation ? "en" : "", expectedAudio, "",
                    "AUDIO_MISSING", "", "false", "MISSING", "true",
                    Boolean.toString(currentScopeIds.contains(segment.id())),
                    validTranslation ? "AUDIO_MISSING" : translationStatus);
        }).collect(Collectors.joining("\n"));
        long translationValid = derivativeMatrix.lines()
                .filter(line -> line.contains("|TRANSLATION_VALID|")).count();
        long translationMissing = derivativeMatrix.lines()
                .filter(line -> line.contains("|TRANSLATION_MISSING|")).count();
        long translationInvalid = derivativeMatrix.lines()
                .filter(line -> line.contains("|TRANSLATION_INVALID|")).count();
        long wavFiles;
        try (var paths = Files.walk(project)) {
            wavFiles = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".wav"))
                    .count();
        }
        Files.writeString(report, "manifestPages=" + manifest.pageCount()
                + "\npreparedPages=" + pages.stream().map(page -> page.pageNumber()).toList()
                + "\nsegments=" + script.segments().size()
                + "\nintervalPages=" + interval.resolvedPageNumbers()
                + "\nintervalSegments=" + interval.resolvedSegmentIds().size()
                + "\nintervalSegmentIds=" + interval.resolvedSegmentIds()
                + "\nfullDocumentTranslationCacheHits=" + fullDocumentTranslationCacheHits
                + "\ntranslationValid=" + translationValid
                + "\ntranslationMissing=" + translationMissing
                + "\ntranslationInvalid=" + translationInvalid
                + "\nphysicalWavFiles=" + wavFiles
                + "\ntypes=" + types + "\nprojectionItems=" + projection.items().size()
                + "\nresultFinalRegion=" + resultRegion.id()
                + "\nresultFinalSegment=" + resultSegment.id()
                + "\ncoverageComplete=" + coverage.complete()
                + "\nbindings=OK\ncanonicalHashes=UNCHANGED\nqwenInvocations=0\n"
                + "sequence|segmentId|page|sourceRegionId|readingOrder|paintedRegionId|bbox|classification\n"
                + String.join("\n", transitions) + "\n"
                + "page|order|regionId|type|coverage|segmentIds|reason\n" + matrix + "\n"
                + "segmentId|sourceBlockId|page|canonicalTextHash|sourceLanguage|targetLanguage|translationCacheStatus|translationFingerprint|translationResultStatus|effectiveTextPresent|effectiveTextHash|effectiveLanguage|audioExpectedFingerprint|audioPersistedFingerprint|audioUnitStatus|audioPath|audioExists|segmentCompositionStatus|includedByFullDocument|includedByCurrentScope|preflightStatus\n"
                + derivativeMatrix + "\n",
                StandardCharsets.UTF_8);
    }

    private static void assertPageTypes(
            com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument script,
            int page, String... expected) {
        var types = script.segments().stream()
                .filter(segment -> Integer.toString(page).equals(
                        segment.metadata().get("pdfSourcePage")))
                .map(segment -> segment.metadata().get("sourceBlockType"))
                .collect(Collectors.toSet());
        for (String type : expected) assertTrue(types.contains(type),
                () -> "P" + page + " no contiene " + type + ": " + types);
    }

    private static Map<Path, String> canonicalHashes(Path project) throws Exception {
        LinkedHashMap<Path, String> hashes = new LinkedHashMap<>();
        try (var paths = Files.list(project.resolve("document/pages"))) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                byte[] digest = MessageDigest.getInstance("SHA-256")
                        .digest(Files.readAllBytes(path));
                hashes.put(path.getFileName(), java.util.HexFormat.of().formatHex(digest));
            }
        }
        return hashes;
    }

    private static String hash(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String bbox(PdfPageRegion region) {
        return String.format(java.util.Locale.ROOT, "%.2f,%.2f,%.2f,%.2f",
                region.xMinPoints(), region.yMinPoints(),
                region.xMaxPoints(), region.yMaxPoints());
    }

    private static String classification(PdfVisualReadingProjection projection,
                                         PdfVisualTextTarget target) {
        PdfPageRegion box = target.region();
        if (box.xMinPoints() < 0 || box.yMinPoints() < 0
                || box.xMaxPoints() > box.pageWidthPoints()
                || box.yMaxPoints() > box.pageHeightPoints()
                || box.xMaxPoints() <= box.xMinPoints()
                || box.yMaxPoints() <= box.yMinPoints()) {
            return "GEOMETRY_SUSPECT";
        }
        if (target.kind() == PdfVisualTextTargetKind.SENTENCE) {
            return projection.highlightForRegion(target.regionId())
                    .filter(block -> sameBox(block.region(), target.region()))
                    .map(ignored -> "COARSE_BUT_CORRECT")
                    .orElse("EXACT");
        }
        return "EXACT";
    }

    private static boolean sameBox(PdfPageRegion left, PdfPageRegion right) {
        double epsilon = 0.01;
        return Math.abs(left.xMinPoints() - right.xMinPoints()) < epsilon
                && Math.abs(left.yMinPoints() - right.yMinPoints()) < epsilon
                && Math.abs(left.xMaxPoints() - right.xMaxPoints()) < epsilon
                && Math.abs(left.yMaxPoints() - right.yMaxPoints()) < epsilon;
    }
}
