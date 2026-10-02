package com.marcosmoreiradev.docupodcaststudio.application.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PdfNativeTextQualityAssessorTest {
    private final PdfNativeTextQualityAssessor assessor = new PdfNativeTextQualityAssessor();

    @Test
    void acceptsCoherentNativeTextWithoutOcr() {
        PdfTextLayer layer = layer("Este párrafo digital contiene texto continuo y geometría válida.");
        assertEquals(PdfNativeTextQuality.RELIABLE, assessor.assess(layer).quality());
    }

    @Test
    void rejectsUnavailableOrEmptyLayer() {
        PdfTextLayer layer = new PdfTextLayer(1, PdfTextLayerOrigin.UNAVAILABLE, List.of(), List.of());
        assertEquals(PdfNativeTextQuality.UNUSABLE, assessor.assess(layer).quality());
    }

    @Test
    void rejectsSevereCharacterFragmentation() {
        assertEquals(PdfNativeTextQuality.UNUSABLE,
                assessor.assess(layer("H O L A M U N D O")).quality());
    }

    private static PdfTextLayer layer(String text) {
        PdfPageRegion region = new PdfPageRegion(1, 10, 20, 500, 60, 612, 792);
        return new PdfTextLayer(1, PdfTextLayerOrigin.NATIVE_BBOX,
                List.of(new PdfTextLine(1, text, region, List.of(), 1.0)), List.of());
    }
}
