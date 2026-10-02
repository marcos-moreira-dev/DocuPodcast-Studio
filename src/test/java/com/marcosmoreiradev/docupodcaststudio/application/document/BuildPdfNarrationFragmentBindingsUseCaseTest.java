package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarrationFragmentBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionEvidence;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOverride;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfSemanticTextLayer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class BuildPdfNarrationFragmentBindingsUseCaseTest {

    @Test
    void persistsDistinctSentenceRangesAndLineGeometryInsideOneRegion() {
        String text = "Primera oración visible. Segunda oración visible.";
        PdfRegion region = region(text, Map.of(
                "lineBboxes", "10,20,300,40;10,42,320,62",
                "lineCharRanges", "0-24;24-49"));

        var bindings = new BuildPdfNarrationFragmentBindingsUseCase().build(
                "SEG", region, text, PdfSemanticTextLayer.LITERAL, 600, 800);

        assertEquals(3, bindings.size());
        var first = bindings.get(1);
        var second = bindings.get(2);
        assertEquals("SEG-U001", first.unitId());
        assertEquals(0, first.sourceTextStart());
        assertEquals(24, first.sourceTextEnd());
        assertEquals(List.of(0), first.sourceLineIndices());
        assertEquals(20, first.playbackBboxes().getFirst().yMin());
        assertEquals(List.of(1), second.sourceLineIndices());
        assertEquals(42, second.playbackBboxes().getFirst().yMin());
        assertNotEquals(first.playbackBboxes(), second.playbackBboxes());
        assertTrue(first.fineGeometry());
    }

    @Test
    void unionsEveryLineTouchedByAMultilineSentence() {
        String text = "Una oración que ocupa dos líneas completas.";
        PdfRegion region = region(text, Map.of(
                "lineBboxes", "10,20,280,40;10,42,330,62",
                "lineCharRanges", "0-20;20-42"));

        var unit = new BuildPdfNarrationFragmentBindingsUseCase().build(
                "SEG", region, text, PdfSemanticTextLayer.LITERAL, 600, 800).get(1);

        assertEquals(List.of(0, 1), unit.sourceLineIndices());
        assertEquals(2, unit.playbackBboxes().size());
    }

    @Test
    void fallsBackToWholeRegionWhenNarrationCannotBeMappedWithoutGuessing() {
        PdfRegion region = region("Dos frases. Otra frase.", Map.of(
                "lineBboxes", "10,20,300,40;10,42,320,62",
                "lineCharRanges", "0-11;11-22"));

        var bindings = new BuildPdfNarrationFragmentBindingsUseCase().build(
                "SEG", region, "Una sola narración reinterpretada.",
                PdfSemanticTextLayer.LITERAL, 600, 800);

        var unit = bindings.get(1);
        assertEquals(PdfNarrationFragmentBinding.GeometryAuthority.REGION_FALLBACK,
                unit.geometryAuthority());
        assertEquals(0, unit.sourceTextStart());
        assertEquals(region.effectiveText().length(), unit.sourceTextEnd());
        assertFalse(unit.fineGeometry());
    }

    @Test
    void codecRoundTripsAndResolvesExactUnitBeforeParentFallback() {
        String text = "Primera. Segunda.";
        var bindings = new BuildPdfNarrationFragmentBindingsUseCase().build(
                "SEG", region(text, Map.of()), text,
                PdfSemanticTextLayer.LITERAL, 600, 800);
        String encoded = PdfNarrationFragmentBindingMetadata.encode(bindings);

        assertEquals(bindings, PdfNarrationFragmentBindingMetadata.decode(encoded));
        assertEquals("SEG-U002", PdfNarrationFragmentBindingMetadata
                .resolve(encoded, "SEG-U002").orElseThrow().unitId());
        assertEquals("SEG", PdfNarrationFragmentBindingMetadata
                .resolve(encoded, "SEG-U999").orElseThrow().unitId());
    }

    private static PdfRegion region(String text, Map<String, String> attributes) {
        var evidence = new PdfRegionEvidence(PdfRegionOrigin.NATIVE_TEXT, 1.0,
                "test", "test", "test", "test");
        return new PdfRegion("PDF-R1", 1, 10, 20, 400, 100, 0, 0, text,
                PdfRegionType.PARAGRAPH, PdfNarratability.NARRATABLE, List.of(),
                evidence, PdfRegionOverride.empty(), attributes, 1);
    }
}
