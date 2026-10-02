package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarrationBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfObjectNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfSemanticTextLayer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfNarrationBindingMetadataTest {

    @Test
    void roundTripsLiteralAndInterpretiveBindingsWithoutLosingProvenance() {
        PdfNarrationBinding literal = new PdfNarrationBinding(
                2, List.of("P2-R1"), PdfSemanticTextLayer.LITERAL,
                PdfObjectNarrationPolicy.READ_EXACT, "", 4,
                "human-override|P2-R1|4");
        PdfNarrationBinding interpretation = new PdfNarrationBinding(
                2, List.of("P2-R2", "P2-R3"),
                PdfSemanticTextLayer.INTERPRETATION,
                PdfObjectNarrationPolicy.DESCRIBE_BRIEFLY,
                "DER-2", 7, "sha|model|revision");

        assertEquals(literal, PdfNarrationBindingMetadata.decode(
                PdfNarrationBindingMetadata.encode(literal)).orElseThrow());
        assertEquals(interpretation, PdfNarrationBindingMetadata.decode(
                PdfNarrationBindingMetadata.encode(interpretation)).orElseThrow());
    }

    @Test
    void rejectsUnknownOrMalformedMetadataVersions() {
        assertTrue(PdfNarrationBindingMetadata.decode("").isEmpty());
        assertTrue(PdfNarrationBindingMetadata.decode("2|broken").isEmpty());
        assertTrue(PdfNarrationBindingMetadata.decode("1|not-a-page").isEmpty());
    }
}
