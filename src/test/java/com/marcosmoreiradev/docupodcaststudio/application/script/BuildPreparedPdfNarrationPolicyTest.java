package com.marcosmoreiradev.docupodcaststudio.application.script;

import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfWorkspaceRef;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRegion;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextLayerOrigin;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualReadingProjection;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualTextTarget;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualTextTargetKind;
import com.marcosmoreiradev.docupodcaststudio.application.document.ResolvePdfPlaybackHighlightUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticReadingPolicy;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPreparedPdfDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class BuildPreparedPdfNarrationPolicyTest {
    @TempDir Path temp;

    @Test
    void narratesCaptionSidebarTableMathAndConsecutiveParagraphsInIdentityOrder()
            throws Exception {
        var built = build(List.of(
                region("P1", 0, PdfRegionType.PARAGRAPH, "Primer párrafo."),
                region("IMG", 1, PdfRegionType.IMAGE, "eje x y curva"),
                region("CAP", 2, PdfRegionType.CAPTION,
                        "Figura uno. La curva converge desde ambos lados."),
                region("BOX", 3, PdfRegionType.SIDEBAR, "Conclusión importante."),
                region("TAB", 4, PdfRegionType.TABLE,
                        "Concepto ; Valor\nRadio ; 1\nÁrea ; 1/2 sin x"),
                region("MATH", 5, PdfRegionType.MATH, "sin x < x < tan x"),
                region("P2", 6, PdfRegionType.PARAGRAPH, "Segundo párrafo.")),
                List.of(imageTreatment("IMG",
                        "La imagen muestra una curva que se aproxima al mismo valor desde la izquierda y la derecha.")));

        assertEquals(List.of("P1", "IMG", "CAP", "BOX", "TAB", "MATH", "P2"),
                built.segments().stream().map(segment ->
                        segment.sourceBlockIds().getFirst()).toList());
        assertTrue(textFor(built, "CAP").contains("converge desde ambos lados"));
        assertTrue(textFor(built, "BOX").startsWith("Recuadro:"));
        assertTrue(textFor(built, "TAB").contains("Concepto: Radio"));
        assertTrue(textFor(built, "TAB").contains("un medio por seno de equis"),
                textFor(built, "TAB"));
        assertFalse(textFor(built, "TAB").contains(";"));
        assertTrue(textFor(built, "MATH").contains("seno de equis menor que equis"));
    }

    @Test
    void removesOnlyAActuallyRedundantAdjacentCaption() throws Exception {
        String speech = "La figura muestra una curva azul que converge al valor uno desde ambos lados.";
        var built = build(List.of(
                region("IMG", 0, PdfRegionType.IMAGE, "curva azul"),
                region("CAP", 1, PdfRegionType.CAPTION,
                        "Figura. Curva azul que converge al valor uno desde ambos lados.")),
                List.of(imageTreatment("IMG", speech)));

        assertEquals(1, built.segments().size());
        assertEquals("IMG", built.segments().getFirst().sourceBlockIds().getFirst());
        assertEquals("CAP", built.segments().getFirst().metadata()
                .get(ValidatePreparedPdfNarrationCoverageUseCase.COVERED_REGION_IDS));
    }

    @Test
    void inferredNonNarratableSidebarWithSubstantiveContentIsStillNarrated() throws Exception {
        PdfRegion sidebar = new PdfRegion("RESULT", 1, 40, 50, 560, 140,
                0, 0, "Resultado final lim??? sin x/x = 1", PdfRegionType.SIDEBAR,
                PdfNarratability.NON_NARRATABLE, List.of("verifier"),
                new PdfRegionEvidence(PdfRegionOrigin.VLM_SEMANTIC, 1.0,
                        "qwen", "block-v1", "reader", "reader"),
                PdfRegionOverride.empty(), Map.of(), 1);

        var built = build(List.of(sidebar), List.of());

        assertEquals(1, built.segments().size());
        assertTrue(built.segments().getFirst().narrationText().contains("Resultado final"));
        assertFalse(built.segments().getFirst().narrationText().contains("?"));
        var report = new ValidatePreparedPdfNarrationCoverageUseCase().validate(
                List.of(new PreparedPdfPage(PreparedPdfPage.CURRENT_SCHEMA_VERSION, 1, 612, 792,
                        PdfPagePreparationStatus.READY, 1, List.of(sidebar), List.of(),
                        PdfPagePreparationMetrics.empty(), PdfPageAnalysisProfile.defaults(), List.of(), "")),
                built, SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF);
        assertTrue(report.complete());
        assertEquals(PdfNarrationCoverageStatus.NARRATED, report.items().getFirst().status());
    }

    @Test
    void realNarrationPolicyDoesNotShiftPlaybackAfterANonNarratableRegion()
            throws Exception {
        List<PdfRegion> regions = List.of(
                region("R1", 0, PdfRegionType.PARAGRAPH, "Primer parrafo narrable."),
                manuallySkippedRegion("R2", 1, "Marca decorativa."),
                region("R3", 2, PdfRegionType.PARAGRAPH, "Segundo parrafo narrable."),
                region("R4", 3, PdfRegionType.IMAGE, "Grafico del limite."),
                region("R5", 4, PdfRegionType.CAPTION, "Fuente y autoria de la figura."),
                region("R6", 5, PdfRegionType.MATH, "sin x < x < tan x"),
                region("R7", 6, PdfRegionType.SIDEBAR, "Resultado final importante."));
        var script = build(regions, List.of(imageTreatment(
                "R4", "La imagen compara tres curvas alrededor del origen.")));
        List<String> expected = List.of("R1", "R3", "R4", "R5", "R6", "R7");
        assertEquals(expected, script.segments().stream()
                .map(segment -> segment.metadata().get("sourceBlockId")).toList());

        List<PdfVisualTextTarget> targets = regions.stream()
                .map(BuildPreparedPdfNarrationPolicyTest::target).toList();
        PdfVisualReadingProjection projection = new PdfVisualReadingProjection(
                List.of(), Map.of(), targets, List.of());
        ResolvePdfPlaybackHighlightUseCase resolver =
                new ResolvePdfPlaybackHighlightUseCase();
        String previousRegion = "";
        for (int index = 0; index < script.segments().size(); index++) {
            var segment = script.segments().get(index);
            String expectedRegion = expected.get(index);
            var actual = resolver.resolveTarget(projection, segment,
                    previousRegion, segment.id() + "-U001").orElseThrow();
            assertEquals(expectedRegion, actual.regionId(), segment.id());
            previousRegion = expectedRegion;
        }
    }

    @Test
    void structuralParentNeverProducesAudioAndLeavesKeepFrozenOrder() throws Exception {
        PdfRegion parent = structuralContainer("SECTION", 0, 30, 35, 580, 500);
        List<PdfRegion> completionOrder = List.of(
                child("MATH", 4, PdfRegionType.MATH, "x al cuadrado", parent),
                child("TITLE", 1, PdfRegionType.TITLE, "Limite notable", parent),
                child("CAP", 3, PdfRegionType.CAPTION, "Figura uno", parent),
                child("P", 2, PdfRegionType.PARAGRAPH, "Explicacion principal.", parent));

        var built = build(java.util.stream.Stream.concat(
                java.util.stream.Stream.of(parent), completionOrder.stream()).toList(), List.of());

        assertEquals(List.of("TITLE", "P", "CAP", "MATH"), built.segments().stream()
                .map(segment -> segment.sourceBlockIds().getFirst()).toList());
        assertFalse(built.segments().stream()
                .anyMatch(segment -> segment.sourceBlockIds().contains("SECTION")));
        assertTrue(built.segments().stream().allMatch(segment ->
                "LEAF".equals(segment.metadata().get("pdfRegionRole"))
                        && "true".equals(segment.metadata().get("pdfPlaybackTarget"))));
    }

    private com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument build(
            List<PdfRegion> regions, List<PdfDerivedTreatment> treatments) throws Exception {
        JsonPreparedPdfDocumentRepository repository = new JsonPreparedPdfDocumentRepository();
        String sha = "9".repeat(64);
        repository.initialize(temp, new PdfDocumentManifest(
                PdfDocumentManifest.CURRENT_SCHEMA_VERSION, "policy", "source.pdf",
                sha, 1, "v1", Instant.now(), Instant.now()));
        repository.savePage(temp, new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION, 1, 612, 792,
                PdfPagePreparationStatus.READY, 1, regions, treatments,
                PdfPagePreparationMetrics.empty(), PdfPageAnalysisProfile.defaults(),
                List.of(), ""));
        return new BuildPreparedPdfNarrationUseCase(repository).buildPage(
                new PreparedPdfWorkspaceRef(temp, temp.resolve("source.pdf"), sha),
                1, "policy", "es", false, null,
                SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF);
    }

    private static PdfRegion region(String id, int order, PdfRegionType type,
                                    String text) {
        return new PdfRegion(id, 1, 40, 50 + order * 60, 560, 95 + order * 60,
                0, order, text, type, PdfNarratability.NARRATABLE, List.of(),
                new PdfRegionEvidence(PdfRegionOrigin.VLM_SEMANTIC, 1.0,
                        "qwen", "block-v1", "reader", "reader"),
                PdfRegionOverride.empty(), Map.of(), 1);
    }

    private static PdfDerivedTreatment imageTreatment(String regionId, String text) {
        return new PdfDerivedTreatment("DER-" + regionId,
                PdfDerivedTreatmentKind.IMAGE_DESCRIPTION, List.of(regionId), text,
                "qwen3-vl:4b-instruct-q8_0", "v1", 1, Instant.now(),
                PdfDerivedTreatmentState.APPROVED, 1, "legacy-source", "prompt",
                Map.of("automaticAdmission", "USER_POLICY_AUTOMATIC",
                        "groundingStatus", "OK", "sourceFingerprintValidated", "true",
                        "ttsSafetyValidated", "true", "semanticStrategy", "BRIEF"));
    }

    private static PdfRegion structuralContainer(String id, int order,
                                                  double x1, double y1,
                                                  double x2, double y2) {
        return new PdfRegion(id, 1, x1, y1, x2, y2, 0, order, "",
                PdfRegionType.SIDEBAR, PdfNarratability.NON_NARRATABLE, List.of(),
                new PdfRegionEvidence(PdfRegionOrigin.VLM_SEMANTIC, 1.0,
                        "qwen", "block-v1", "reader", "reader"),
                PdfRegionOverride.empty(), Map.of("regionRole", "CONTAINER",
                "playbackTarget", "false"), 1);
    }

    private static PdfRegion child(String id, int order, PdfRegionType type,
                                   String text, PdfRegion parent) {
        double y = 45 + order * 70;
        return new PdfRegion(id, 1, 45, y, 565, y + 45, 0, order, text,
                type, PdfNarratability.NARRATABLE, List.of(),
                new PdfRegionEvidence(PdfRegionOrigin.VLM_SEMANTIC, 1.0,
                        "qwen", "block-v1", "reader", "reader"),
                PdfRegionOverride.empty(), Map.of("regionRole", "LEAF",
                "playbackTarget", "true", "parentId", parent.id()), 1);
    }

    private static PdfRegion manuallySkippedRegion(String id, int order, String text) {
        return new PdfRegion(id, 1, 40, 50 + order * 60, 560, 95 + order * 60,
                0, order, text, PdfRegionType.PARAGRAPH,
                PdfNarratability.NON_NARRATABLE, List.of(),
                new PdfRegionEvidence(PdfRegionOrigin.VLM_SEMANTIC, 1.0,
                        "qwen", "block-v1", "reader", "reader"),
                new PdfRegionOverride(null, null,
                        PdfNarratability.NON_NARRATABLE, null), Map.of(), 1);
    }

    private static PdfVisualTextTarget target(PdfRegion region) {
        PdfPageRegion box = new PdfPageRegion(region.pageNumber(), region.xMin(),
                region.yMin(), region.xMax(), region.yMax(), 612, 792);
        return new PdfVisualTextTarget(region.id() + "-B", region.id(),
                0, region.effectiveText().length(), region.pageNumber(), box,
                List.of(box), List.of(region.id()), region.effectiveText(),
                PdfTextLayerOrigin.NATIVE_BBOX, PdfVisualTextTargetKind.BLOCK);
    }

    private static String textFor(
            com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument script,
            String sourceId) {
        return script.segments().stream()
                .filter(segment -> segment.sourceBlockIds().contains(sourceId))
                .findFirst().orElseThrow().narrationText();
    }
}
