package com.marcosmoreiradev.docupodcaststudio.acceptance;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfRegionContentResolver;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfSemanticPageAnalysis;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextLayer;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextLine;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfWorkspaceRef;
import com.marcosmoreiradev.docupodcaststudio.application.script.BuildPreparedPdfNarrationUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageRole;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticReadingPolicy;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPreparedPdfDocumentRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.PdfBoxNativeTextEvidenceExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Read-only physical gate: no VLM/OCR calls and no project mutations. */
@EnabledIfSystemProperty(named = "docupodcast.pdf.playbackGrounding", matches = "true")
final class PdfPlaybackGroundingPhysicalTest {

    @Test
    void p1AndP2UseNativeLineEvidenceWithoutChangingSemanticRegions() throws Exception {
        Path pdf = Path.of(System.getProperty("docupodcast.pdf.grounding.source",
                "D:/Proyectos/Demostracion_limite_notable.pdf"));
        Path project = Path.of(System.getProperty("docupodcast.pdf.grounding.project",
                "D:/Proyectos/Demostracion_limite_notable"));
        Path report = Path.of("target/pdf-playback-grounding/physical-report.txt");
        Files.createDirectories(report.getParent());

        var repository = new JsonPreparedPdfDocumentRepository();
        var extractor = new PdfBoxNativeTextEvidenceExtractor();
        var resolver = new PdfRegionContentResolver();
        ArrayList<String> output = new ArrayList<>();
        int exact = 0;
        int coarse = 0;
        int unusable = 0;
        LinkedHashMap<String, RegionAudit> audits = new LinkedHashMap<>();

        for (int pageNumber : List.of(1, 2)) {
            var page = repository.loadPage(project, pageNumber).orElseThrow();
            var elements = page.regions().stream().map(region -> element(page.widthPoints(),
                    page.heightPoints(), region)).toList();
            var analysis = new PdfSemanticPageAnalysis(pageNumber, "es", PdfPageRole.CONTENT,
                    elements, 1.0, List.of());
            PdfTextLayer nativeLayer = extractor.extract(pdf, pageNumber);
            var resolved = resolver.resolve(analysis, nativeLayer).analysis();
            assertEquals(page.regions().size(), resolved.elements().size());

            output.add("PAGE=" + pageNumber);
            for (int index = 0; index < page.regions().size(); index++) {
                PdfRegion before = page.regions().get(index);
                var after = resolved.elements().get(index);
                audits.put(before.id(), new RegionAudit(before, after,
                        candidates(before, nativeLayer)));
                assertEquals(elements.get(index).box(), after.box(),
                        "grounding auxiliar no puede mutar el bbox semantico");
                double areaRatio = (before.xMax() - before.xMin())
                        * (before.yMax() - before.yMin())
                        / (page.widthPoints() * page.heightPoints());
                boolean grounded = !after.attributes().getOrDefault(
                        "playbackBboxes", "").isBlank();
                String classification;
                if (areaRatio >= 0.45) {
                    classification = "UNUSABLE";
                    unusable++;
                } else if (grounded || switch (before.effectiveType()) {
                    case IMAGE, TABLE -> areaRatio < 0.10;
                    default -> false;
                }) {
                    classification = "EXACT";
                    exact++;
                } else {
                    classification = "COARSE_BUT_USEFUL";
                    coarse++;
                }
                output.add(String.format(Locale.ROOT,
                        "%s|%s|order=%d|bbox=%.3f,%.3f,%.3f,%.3f|area=%.5f|route=%s|authority=%s|grounding=%s|source=%s",
                        classification, before.effectiveType(), before.effectiveReadingOrder(),
                        before.xMin(), before.yMin(), before.xMax(), before.yMax(), areaRatio,
                        after.attributes().getOrDefault("contentRoute", ""),
                        after.attributes().getOrDefault("layoutAuthority", "unknown"),
                        after.attributes().getOrDefault("playbackGeometryOrigin", "SEMANTIC_BBOX"),
                        summarize(before.effectiveText())));
            }
        }
        output.add("TOTAL|EXACT=" + exact + "|COARSE_BUT_USEFUL=" + coarse
                + "|UNUSABLE=" + unusable + "|VLM_CALLS=0|OCR_CALLS=0");
        Files.write(report, output, StandardCharsets.UTF_8);
        assertEquals(0, unusable);
        assertTrue(exact > 0);

        var manifest = repository.loadManifest(project).orElseThrow();
        var workspace = new PreparedPdfWorkspaceRef(project,
                project.resolve("source/Demostracion_limite_notable.pdf"),
                manifest.sourceSha256());
        var script = new BuildPreparedPdfNarrationUseCase(repository).build(
                workspace, manifest.title(), "es", false, null,
                SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF);
        List<String> desired = List.of("TITLE", "SUBHEADING", "PARAGRAPH", "MATH", "IMAGE");
        ArrayList<String> details = new ArrayList<>();
        for (String type : desired) {
            var segment = script.segments().stream().filter(value -> type.equals(
                    value.metadata().getOrDefault("sourceBlockType", "")))
                    .findFirst().orElseThrow();
            String regionId = segment.metadata().getOrDefault("sourceBlockId",
                    segment.sourceBlockIds().getFirst());
            RegionAudit audit = audits.get(regionId);
            assertTrue(audit != null, regionId);
            String playback = audit.after().attributes().getOrDefault(
                    "playbackBboxes", semantic(audit.region()));
            details.add("segmentId=" + segment.id());
            details.add("spokenText=" + oneLine(segment.narrationText()));
            details.add("sourceBlockId=" + regionId);
            details.add("PdfRegion.type=" + audit.region().effectiveType());
            details.add("semanticBBox=" + semantic(audit.region()));
            details.add("SOURCE=" + oneLine(audit.region().effectiveText()));
            details.add("playbackBboxes.candidates=" + audit.candidates().stream()
                    .map(line -> box(line) + " => " + oneLine(line.text())).toList());
            details.add("lineBboxes=" + audit.after().attributes()
                    .getOrDefault("lineBboxes", ""));
            details.add("lineCharRanges=" + audit.after().attributes()
                    .getOrDefault("lineCharRanges", ""));
            details.add("playbackBBox.chosen=" + playback);
            details.add("reason=" + audit.after().attributes().getOrDefault(
                    "playbackGeometryReason", "persisted-or-semantic"));
            details.add("fallback=" + audit.after().attributes().getOrDefault(
                    "playbackGeometryFallback", Boolean.toString(!audit.after()
                            .attributes().containsKey("playbackBboxes"))));
            details.add("");
            if ("TITLE".equals(type)) {
                assertTrue(playback.contains("43.937,39.912,430.558,50.225"), playback);
                assertTrue(playback.lines().noneMatch(line -> line.contains("565.512")), playback);
            }
        }
        Files.write(Path.of("target/pdf-playback-grounding/segment-audit.txt"),
                details, StandardCharsets.UTF_8);
    }

    private static PdfSemanticPageAnalysis.Element element(double width, double height,
                                                            PdfRegion region) {
        var attrs = new LinkedHashMap<>(region.attributes());
        return new PdfSemanticPageAnalysis.Element(region.id(), region.effectiveReadingOrder(),
                region.effectiveType(), new PdfSemanticPageAnalysis.NormalizedBox(
                region.xMin() / width * 1000.0, region.yMin() / height * 1000.0,
                region.xMax() / width * 1000.0, region.yMax() / height * 1000.0),
                region.effectiveText(), "", region.effectiveNarratability(),
                region.evidence().confidence(), region.reasons(), attrs);
    }

    private static String summarize(String value) {
        String oneLine = value == null ? "" : value.replaceAll("\\s+", " ").strip();
        return oneLine.length() <= 80 ? oneLine : oneLine.substring(0, 77) + "...";
    }

    private static List<PdfTextLine> candidates(PdfRegion region, PdfTextLayer layer) {
        return layer.lines().stream().filter(line -> {
            double x = (line.region().xMinPoints() + line.region().xMaxPoints()) / 2.0;
            double y = (line.region().yMinPoints() + line.region().yMaxPoints()) / 2.0;
            return x >= region.xMin() && x <= region.xMax()
                    && y >= region.yMin() && y <= region.yMax();
        }).toList();
    }

    private static String semantic(PdfRegion region) {
        return String.format(Locale.ROOT, "%.3f,%.3f,%.3f,%.3f",
                region.xMin(), region.yMin(), region.xMax(), region.yMax());
    }

    private static String box(PdfTextLine line) {
        return String.format(Locale.ROOT, "%.3f,%.3f,%.3f,%.3f",
                line.region().xMinPoints(), line.region().yMinPoints(),
                line.region().xMaxPoints(), line.region().yMaxPoints());
    }

    private static String oneLine(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").strip();
    }

    private record RegionAudit(PdfRegion region,
                               PdfSemanticPageAnalysis.Element after,
                               List<PdfTextLine> candidates) { }

}
