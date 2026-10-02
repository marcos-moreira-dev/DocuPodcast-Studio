package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class PdfNarrationTextLayersTest {

    @Test
    void keepsLiteralInterpretationAndNarrationIndependent() {
        PdfNarrationTextLayers layers = new PdfNarrationTextLayers(
                "x²", "x al cuadrado", "Se lee equis al cuadrado.",
                PdfSemanticTextLayer.INTERPRETATION);

        assertEquals("x²", layers.literalText());
        assertEquals("x al cuadrado", layers.interpretationText());
        assertEquals("Se lee equis al cuadrado.", layers.narrationText());
        assertEquals(PdfSemanticTextLayer.INTERPRETATION,
                layers.narrationSourceLayer());
    }

    @Test
    void refusesToClaimAContributorLayerWithoutItsText() {
        assertThrows(IllegalArgumentException.class,
                () -> new PdfNarrationTextLayers(
                        "", "interpretación", "narración",
                        PdfSemanticTextLayer.LITERAL));
        assertThrows(IllegalArgumentException.class,
                () -> new PdfNarrationTextLayers(
                        "literal", "", "narración",
                        PdfSemanticTextLayer.INTERPRETATION));
    }
}
