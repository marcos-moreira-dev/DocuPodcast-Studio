package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarrationBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarrationFragmentBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageGeometry;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfObjectNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfSemanticTextLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ResolvePdfPlaybackHighlightUseCaseTest {
    private final ResolvePdfPlaybackHighlightUseCase resolver =
            new ResolvePdfPlaybackHighlightUseCase();

    @Test
    void resolvesLiteralCueToItsSentenceBoxes() {
        Fixture fixture = fixture(PdfSemanticTextLayer.LITERAL);

        PdfVisualTextHighlight highlight = resolver.resolve(
                fixture.projection(), fixture.segment(), "R1",
                fixture.segment().id() + "-U002").orElseThrow();

        assertEquals("Segunda oración.", highlight.text());
        assertEquals(2, highlight.regions().size());
        assertEquals(200.0, highlight.regions().getFirst().yMinPoints());
    }

    @Test
    void interpretiveCueKeepsTheCompleteObjectHighlight() {
        Fixture fixture = fixture(PdfSemanticTextLayer.INTERPRETATION);

        PdfVisualTextHighlight highlight = resolver.resolve(
                fixture.projection(), fixture.segment(), "R1",
                fixture.segment().id() + "-U002").orElseThrow();

        assertEquals("Bloque completo", highlight.text());
        assertEquals(1, highlight.regions().size());
        assertEquals(50.0, highlight.region().yMinPoints());
    }

    @Test
    void exposesTheExactTargetThatPlaybackMustKeepSelected() {
        Fixture literal = fixture(PdfSemanticTextLayer.LITERAL);
        assertEquals("Segunda oración.", resolver.resolveTarget(
                literal.projection(), literal.segment(), "R1",
                literal.segment().id() + "-U002").orElseThrow().text());

        PdfPageRegion componentBox = box(30, 40, 570, 340);
        PdfVisualTextTarget component = new PdfVisualTextTarget(
                "OBJ-1", "R1", 0, 16, 1, componentBox,
                List.of(componentBox), List.of("R1", "R2"),
                "Figura completa", PdfTextLayerOrigin.NATIVE_BBOX,
                PdfVisualTextTargetKind.SEMANTIC_COMPONENT);
        PdfVisualReadingProjection projection = new PdfVisualReadingProjection(
                List.of(), Map.of(), List.of(component), List.of());
        Fixture interpretation = fixture(PdfSemanticTextLayer.INTERPRETATION);

        assertEquals(component, resolver.resolveTarget(
                projection, interpretation.segment(), "R1",
                interpretation.segment().id() + "-U002").orElseThrow());
    }

    @Test
    void bindingIdentityWinsOverAStaleNeighborRegionFromTheUi() {
        Fixture fixture = fixture(PdfSemanticTextLayer.LITERAL);

        PdfVisualTextHighlight highlight = resolver.resolve(
                fixture.projection(), fixture.segment(), "STALE-PREVIOUS-REGION",
                fixture.segment().id() + "-U002").orElseThrow();

        assertEquals("Segunda oración.", highlight.text());
        assertEquals("R1", ResolvePdfPlaybackHighlightUseCase
                .authoritativeRegionId(fixture.segment(), "STALE-PREVIOUS-REGION"));
    }

    @Test
    void persistedFragmentRangeWinsOverRecomputedUnitOrdinal() {
        Fixture fixture = fixture(PdfSemanticTextLayer.LITERAL);
        PdfPageGeometry exactBox = new PdfPageGeometry(70, 260, 250, 278,
                612, 792, PdfPageGeometry.CANONICAL_SPACE);
        var exact = new PdfNarrationFragmentBinding("SEG-1-U002", "SEG-1", "R1", "R1",
                1, 7, 15, List.of(4), List.of(), List.of(exactBox), "fragmento exacto",
                PdfNarrationFragmentBinding.GeometryAuthority.LINE_CHAR_RANGES);
        NarrationSegment segment = new NarrationSegment(
                fixture.segment().id(), fixture.segment().type(), fixture.segment().title(),
                fixture.segment().narrationText(), fixture.segment().sourceBlockIds(),
                fixture.segment().characterId(), fixture.segment().voiceProfileId(),
                fixture.segment().performanceStyleId(), Map.of(
                PdfNarrationBindingMetadata.KEY, fixture.segment().metadata().get(
                        PdfNarrationBindingMetadata.KEY),
                PdfNarrationFragmentBindingMetadata.KEY,
                PdfNarrationFragmentBindingMetadata.encode(List.of(exact)),
                "sourceBlockId", "R1"));

        PdfVisualTextTarget target = resolver.resolveTarget(fixture.projection(), segment,
                "R1", "SEG-1-U002").orElseThrow();

        assertEquals("fragmento exacto", target.text());
        assertEquals(7, target.startOffset());
        assertEquals(260, target.highlightRegions().getFirst().yMinPoints());
    }

    @Test
    void explicitPrimaryIdentityWinsOverPreviousAndCoveredRegions() {
        PdfPageRegion box = box(40, 50, 560, 300);
        PdfVisualReadingProjection projection = new PdfVisualReadingProjection(
                List.of(), Map.of(), List.of(
                target("R-PREVIOUS", box),
                target("R-PRIMARY", box),
                target("R-COVERED", box)), List.of());
        PdfNarrationBinding binding = new PdfNarrationBinding(
                1, List.of("R-PREVIOUS", "R-PRIMARY", "R-COVERED"),
                PdfSemanticTextLayer.INTERPRETATION,
                PdfObjectNarrationPolicy.DESCRIBE_BRIEFLY,
                "DER-1", 1, "fingerprint");
        NarrationSegment segment = new NarrationSegment(
                "SEG-COMPOUND", NarrationSegmentType.PARAGRAPH, "Compuesto",
                "Narracion interpretativa.",
                List.of("R-PREVIOUS", "R-PRIMARY", "R-COVERED"),
                "CHR-NARRATOR", "VOC-NARRATOR", "STY-NEUTRAL",
                Map.of(
                        PdfNarrationBindingMetadata.KEY,
                        PdfNarrationBindingMetadata.encode(binding),
                        "sourceBlockId", "R-PRIMARY",
                        "pdfCoveredRegionIds", "R-COVERED"));

        PdfVisualTextTarget resolved = resolver.resolveTarget(
                projection, segment, "R-PREVIOUS", "SEG-COMPOUND-U001")
                .orElseThrow();

        assertEquals("R-PRIMARY", resolved.regionId());
        assertEquals("R-PRIMARY",
                ResolvePdfPlaybackHighlightUseCase.primaryRegionId(segment));
        assertNotEquals("R-COVERED", resolved.regionId());
    }

    @Test
    void consecutivePlaybackNeverUsesThePreviousSegmentsRegion() {
        PdfPageRegion box = box(40, 50, 560, 300);
        PdfVisualReadingProjection projection = new PdfVisualReadingProjection(
                List.of(), Map.of(), List.of(
                target("R1", box), target("R2", box), target("R3", box),
                target("R4", box), target("R5", box), target("R6", box),
                target("R7", box)), List.of());
        String previous = "";
        for (int index = 1; index <= 7; index++) {
            String expected = "R" + index;
            PdfNarrationBinding binding = new PdfNarrationBinding(
                    1, List.of(expected), PdfSemanticTextLayer.LITERAL,
                    PdfObjectNarrationPolicy.READ_EXACT, "", index, "fp-" + index);
            NarrationSegment segment = new NarrationSegment(
                    "SEG-" + index, NarrationSegmentType.PARAGRAPH, expected,
                    "Texto " + expected, List.of(expected),
                    "CHR-NARRATOR", "VOC-NARRATOR", "STY-NEUTRAL",
                    Map.of(PdfNarrationBindingMetadata.KEY,
                            PdfNarrationBindingMetadata.encode(binding),
                            "sourceBlockId", expected));

            PdfVisualTextTarget resolved = resolver.resolveTarget(
                    projection, segment, previous, segment.id() + "-U001")
                    .orElseThrow();
            assertEquals(expected, resolved.regionId(), segment.id());
            previous = expected;
        }
    }

    @Test
    void structuralContainerMetadataCannotFallBackToAVisibleTarget() {
        PdfPageRegion box = box(0, 0, 612, 792);
        PdfVisualReadingProjection projection = new PdfVisualReadingProjection(
                List.of(), Map.of(), List.of(target("SECTION", box)), List.of());
        NarrationSegment staleContainer = new NarrationSegment(
                "SEG-SECTION", NarrationSegmentType.PARAGRAPH, "Contenedor",
                "Texto obsoleto", List.of("SECTION"), "CHR", "VOC", "STY",
                Map.of("sourceBlockId", "SECTION", "pdfRegionRole", "CONTAINER",
                        "pdfPlaybackTarget", "false"));

        assertTrue(resolver.resolveTarget(projection, staleContainer,
                "SECTION", "SEG-SECTION-U001").isEmpty());
        assertTrue(resolver.resolve(projection, staleContainer,
                "SECTION", "SEG-SECTION-U001").isEmpty());
    }

    private static Fixture fixture(PdfSemanticTextLayer layer) {
        PdfPageRegion block = box(40, 50, 560, 300);
        PdfPageRegion first = box(60, 80, 400, 95);
        PdfPageRegion secondLineOne = box(60, 200, 420, 215);
        PdfPageRegion secondLineTwo = box(60, 220, 300, 235);
        PdfVisualTextTarget firstTarget = new PdfVisualTextTarget(
                "R1-S-0", "R1", 0, 18, 1, first, List.of(first),
                "Primera oración.", PdfTextLayerOrigin.NATIVE_BBOX,
                PdfVisualTextTargetKind.SENTENCE);
        PdfVisualTextTarget secondTarget = new PdfVisualTextTarget(
                "R1-S-19", "R1", 19, 37, 1,
                box(60, 200, 420, 235),
                List.of(secondLineOne, secondLineTwo),
                "Segunda oración.", PdfTextLayerOrigin.NATIVE_BBOX,
                PdfVisualTextTargetKind.SENTENCE);
        PdfVisualReadingProjection projection = new PdfVisualReadingProjection(
                List.of(), Map.of("R1", new PdfVisualTextHighlight(
                1, block, "Bloque completo", PdfTextLayerOrigin.NATIVE_BBOX)),
                List.of(firstTarget, secondTarget), List.of());
        String interpretationId = layer == PdfSemanticTextLayer.INTERPRETATION
                ? "DER-1" : "";
        PdfNarrationBinding binding = new PdfNarrationBinding(
                1, List.of("R1"), layer,
                layer == PdfSemanticTextLayer.INTERPRETATION
                        ? PdfObjectNarrationPolicy.DESCRIBE_BRIEFLY
                        : PdfObjectNarrationPolicy.READ_EXACT,
                interpretationId, 1, "fingerprint");
        NarrationSegment segment = new NarrationSegment(
                "SEG-1", NarrationSegmentType.PARAGRAPH, "Segmento",
                "Primera oración. Segunda oración.", List.of("R1"),
                "CHR-NARRATOR", "VOC-NARRATOR", "STY-NEUTRAL",
                Map.of(PdfNarrationBindingMetadata.KEY,
                        PdfNarrationBindingMetadata.encode(binding)));
        return new Fixture(projection, segment);
    }

    private static PdfPageRegion box(
            double xMin, double yMin, double xMax, double yMax) {
        return new PdfPageRegion(
                1, xMin, yMin, xMax, yMax, 612, 792);
    }

    private static PdfVisualTextTarget target(String regionId, PdfPageRegion box) {
        return new PdfVisualTextTarget(
                regionId + "-B", regionId, 0, 10, 1, box,
                List.of(box), List.of(regionId), regionId,
                PdfTextLayerOrigin.NATIVE_BBOX, PdfVisualTextTargetKind.BLOCK);
    }

    private record Fixture(
            PdfVisualReadingProjection projection,
            NarrationSegment segment) {
    }
}
