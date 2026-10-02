package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPagePreparationStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionEvidence;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOverride;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildPdfVisualReadingProjectionUseCaseTest {
    @TempDir Path temp;

    @Test
    void createsBlockAndSentenceTargetsFromCanonicalNarratableRegion() {
        Fixture fixture = fixture(region("R1", "Primera oración. Segunda oración.",
                PdfRegionType.PARAGRAPH, PdfNarratability.NARRATABLE));

        PdfVisualReadingProjection projection = fixture.useCase().build(fixture.workspace());

        assertEquals(3, projection.targets().size());
        assertEquals(2, projection.targets().stream()
                .filter(target -> target.kind() == PdfVisualTextTargetKind.SENTENCE).count());
        assertTrue(projection.targetAt(1, 80, 108).isPresent());
        PdfVisualTextTarget firstSentence = projection.targets().stream()
                .filter(target -> target.kind() == PdfVisualTextTargetKind.SENTENCE)
                .findFirst().orElseThrow();
        assertTrue(projection.targetForSelection(PdfRegionSelectionRef.from(firstSentence)).isPresent());
        assertTrue(projection.highlightForRegion("R1").isPresent());
    }

    @Test
    void exposesNonNarratableMathAsOneWholeSemanticComponent() {
        Fixture fixture = fixture(region("M1", "x = a + b / 2",
                PdfRegionType.MATH, PdfNarratability.NON_NARRATABLE));

        PdfVisualReadingProjection projection = fixture.useCase().build(fixture.workspace());

        assertEquals(1, projection.targets().size());
        assertEquals(PdfVisualTextTargetKind.SEMANTIC_COMPONENT,
                projection.targets().getFirst().kind());
        assertTrue(projection.highlightForRegion("M1").isPresent());
    }

    @Test
    void semanticComponentWinsHitTestingOverInternalTextTargets() {
        PdfPageRegion componentBox = new PdfPageRegion(
                1, 50, 80, 500, 300, 612, 792);
        PdfPageRegion sentenceBox = new PdfPageRegion(
                1, 80, 100, 420, 142, 612, 792);
        PdfVisualTextTarget component = new PdfVisualTextTarget(
                "OBJ-1", "R1", 0, 20, 1, componentBox,
                List.of(componentBox), List.of("R1", "R2"), "Tabla",
                PdfTextLayerOrigin.NATIVE_BBOX,
                PdfVisualTextTargetKind.SEMANTIC_COMPONENT);
        PdfVisualTextTarget sentence = new PdfVisualTextTarget(
                "S-1", "R1", 0, 20, 1, sentenceBox,
                "Texto interior", PdfTextLayerOrigin.NATIVE_BBOX,
                PdfVisualTextTargetKind.SENTENCE);
        PdfVisualReadingProjection projection = new PdfVisualReadingProjection(
                List.of(), Map.of(), List.of(sentence, component), List.of());

        assertEquals(component, projection.targetAt(1, 100, 110).orElseThrow());
        assertEquals(List.of("R1", "R2"),
                projection.targetAt(1, 100, 110).orElseThrow()
                        .sourceRegionIds());
    }

    @Test
    void usesOnlyTheMatchingLineGeometryForEachSentenceHighlight() {
        String text = "Primera oración. Segunda oración.";
        int secondStart = text.indexOf("Segunda");
        Map<String, String> attributes = Map.of(
                "lineBboxes", "72,100,420,112;72,130,420,142",
                "lineCharRanges", "0-" + (secondStart - 1) + ";"
                        + secondStart + "-" + text.length());
        Fixture fixture = fixture(region("R2", text,
                PdfRegionType.PARAGRAPH, PdfNarratability.NARRATABLE,
                attributes));

        List<PdfVisualTextTarget> sentences = fixture.useCase()
                .build(fixture.workspace()).targets().stream()
                .filter(target -> target.kind() == PdfVisualTextTargetKind.SENTENCE)
                .toList();

        assertEquals(2, sentences.size());
        assertEquals(112.0, sentences.getFirst().region().yMaxPoints());
        assertEquals(130.0, sentences.getLast().region().yMinPoints());
        assertEquals(1, sentences.getFirst().highlightRegions().size());
        assertEquals(sentences.getLast(), fixture.useCase().build(fixture.workspace())
                .sentenceTargetForRegion("R2", 1).orElseThrow());
    }

    @Test
    void hitTestingFallsBackToTheWholeBlockWhenSentenceHasNoOwnGeometry() {
        Fixture fixture = fixture(region("R-FALLBACK",
                "Primera oración. Segunda oración.",
                PdfRegionType.PARAGRAPH, PdfNarratability.NARRATABLE));

        PdfVisualTextTarget selected = fixture.useCase().build(fixture.workspace())
                .targetAt(1, 80, 108).orElseThrow();

        assertEquals(PdfVisualTextTargetKind.BLOCK, selected.kind());
        assertEquals("Primera oración. Segunda oración.", selected.text());
    }

    @Test
    void keepsOneMultilineSentenceAsMultipleHighlightBoxes() {
        String text = "Una oración comienza en esta línea y continúa "
                + "en la línea siguiente.";
        int secondStart = text.indexOf("en la línea siguiente");
        Map<String, String> attributes = Map.of(
                "lineBboxes", "72,100,420,112;72,130,300,142",
                "lineCharRanges", "0-" + secondStart + ";"
                        + secondStart + "-" + text.length());
        Fixture fixture = fixture(region("R3", text,
                PdfRegionType.PARAGRAPH, PdfNarratability.NARRATABLE,
                attributes));

        PdfVisualTextTarget sentence = fixture.useCase()
                .build(fixture.workspace())
                .sentenceTargetForRegion("R3", 0).orElseThrow();

        assertEquals(2, sentence.highlightRegions().size());
        assertEquals(100.0,
                sentence.highlightRegions().getFirst().yMinPoints());
        assertEquals(130.0,
                sentence.highlightRegions().getLast().yMinPoints());
        assertEquals(2, sentence.highlight().regions().size());
        assertTrue(!sentence.contains(350, 121, 0),
                "el espacio vacío entre líneas no debe comportarse como caja");
        assertEquals(100.0, sentence.highlight().visibleRegion().yMinPoints());
        assertEquals(142.0, sentence.highlight().visibleRegion().yMaxPoints());
        assertEquals(72.0, sentence.highlight().visibleRegion().xMinPoints());
        assertEquals(420.0, sentence.highlight().visibleRegion().xMaxPoints());
    }

    @Test
    void usesAuxiliaryPlaybackBoxesWithoutChangingSemanticRegion() {
        Map<String, String> attributes = Map.of(
                "playbackBboxes", "80,106,300,116;80,120,360,130",
                "playbackGeometryOrigin", "NATIVE_TEXT_LINES");
        Fixture fixture = fixture(region("R-GROUND", "Texto VLM estable",
                PdfRegionType.MATH, PdfNarratability.NON_NARRATABLE, attributes));

        PdfVisualReadingProjection projection = fixture.useCase().build(fixture.workspace());
        PdfVisualTextTarget target = projection.targets().getFirst();

        assertEquals(72.0, target.region().xMinPoints(),
                "la caja semántica canónica no se modifica");
        assertEquals(2, target.highlightRegions().size());
        assertEquals(106.0, target.highlightRegions().getFirst().yMinPoints());
        assertEquals(2, projection.highlightForRegion("R-GROUND")
                .orElseThrow().regions().size());
    }

    @Test
    void malformedOrOutOfBoundsPlaybackGeometryFallsBackToSemanticBox() {
        Fixture fixture = fixture(region("R-BAD", "Texto",
                PdfRegionType.MATH, PdfNarratability.NON_NARRATABLE,
                Map.of("playbackBboxes", "0,0,600,700")));

        PdfVisualTextTarget target = fixture.useCase().build(fixture.workspace())
                .targets().getFirst();

        assertEquals(1, target.highlightRegions().size());
        assertEquals(target.region(), target.highlightRegions().getFirst());
    }

    @Test
    void rasterLineWithoutWordEvidenceFallsBackToSemanticBox() {
        Fixture fixture = fixture(region("R-OCR", "Texto OCR",
                PdfRegionType.PARAGRAPH, PdfNarratability.NARRATABLE,
                Map.of("playbackBboxes", "60,95,500,150",
                        "playbackGeometryOrigin", "OCR_RASTER_PAGE")));

        PdfVisualTextTarget target = fixture.useCase().build(fixture.workspace())
                .targets().getFirst();

        assertEquals(72.0, target.highlightRegions().getFirst().xMinPoints());
        assertEquals(142.0, target.highlightRegions().getFirst().yMaxPoints());
    }

    @Test
    void blockTargetUsesOcrWordEnvelopeButKeepsSemanticHitRegion() {
        Fixture fixture = fixture(region("R-WORDS", "Una línea corta",
                PdfRegionType.PARAGRAPH, PdfNarratability.NARRATABLE,
                Map.of(PdfTextVisualBoundsResolver.OCR_WORD_BBOXES_ATTRIBUTE,
                        "80,106,120,118;126,106,205,118",
                        "textGeometryOrigin", "OCR_RASTER_PAGE")));

        PdfVisualTextTarget target = fixture.useCase().build(fixture.workspace())
                .targetForRegion("R-WORDS").orElseThrow();

        assertEquals(72.0, target.region().xMinPoints());
        assertEquals(420.0, target.region().xMaxPoints());
        assertEquals(80.0, target.highlight().visibleRegion().xMinPoints());
        assertEquals(205.0, target.highlight().visibleRegion().xMaxPoints());
        assertEquals(PdfTextLayerOrigin.OCR_LOCAL, target.origin());
    }

    @Test
    void emptyWorkspaceHasNoCompatibilityBlockFallback() {
        Fixture fixture = fixture();
        PdfVisualReadingProjection projection = fixture.useCase().build(fixture.workspace());
        assertTrue(projection.targets().isEmpty());
        assertTrue(projection.warnings().stream().anyMatch(value -> value.contains("V2")));
    }

    @Test
    void structuralContainerHasNoHighlightOrPlaybackTarget() {
        Fixture fixture = fixture(region("SECTION", "", PdfRegionType.SIDEBAR,
                PdfNarratability.NON_NARRATABLE,
                Map.of("regionRole", "CONTAINER", "playbackTarget", "false")));

        PdfVisualReadingProjection projection = fixture.useCase().build(fixture.workspace());

        assertTrue(projection.targets().isEmpty());
        assertTrue(projection.highlightForRegion("SECTION").isEmpty());
    }

    private Fixture fixture(PdfRegion... regions) {
        InMemoryPreparedPdfDocumentRepository repository = new InMemoryPreparedPdfDocumentRepository();
        PreparedPdfWorkspaceRef workspace =
                PreparedPdfTestFixtures.workspace(repository, temp.resolve("workspace"), 1);
        if (regions.length > 0) {
            repository.savePage(workspace.projectRoot(), new PreparedPdfPage(
                    PreparedPdfPage.CURRENT_SCHEMA_VERSION, 1, 612, 792,
                    PdfPagePreparationStatus.READY, 1, List.of(regions), List.of(), ""));
        }
        return new Fixture(workspace, new BuildPdfVisualReadingProjectionUseCase(repository));
    }

    private static PdfRegion region(String id, String text, PdfRegionType type,
                                    PdfNarratability narratability) {
        return region(id, text, type, narratability, Map.of());
    }

    private static PdfRegion region(String id, String text, PdfRegionType type,
                                    PdfNarratability narratability,
                                    Map<String, String> attributes) {
        return new PdfRegion(id, 1, 72, 100, 420, 142, 0, 0, text, type,
                narratability, List.of("test"),
                new PdfRegionEvidence(PdfRegionOrigin.OCR_LOCAL, 0.95,
                        "test", "test", "test", "test"),
                PdfRegionOverride.empty(), attributes, 1);
    }

    private record Fixture(PreparedPdfWorkspaceRef workspace,
                           BuildPdfVisualReadingProjectionUseCase useCase) {
    }
}
