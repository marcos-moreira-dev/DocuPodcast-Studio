package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarrationFocusRef;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfNarrationFocusMetadataTest {
    @Test
    void roundTripsMultipleIndependentBoxesAndAccessibleLabel() {
        PdfNarrationFocusRef source = new PdfNarrationFocusRef(12, List.of("R1", "R2"),
                List.of(new PdfNarrationFocusRef.FocusBox(10, 20, 110, 220),
                        new PdfNarrationFocusRef.FocusBox(200, 40, 350, 280)),
                "Zona narrada");

        PdfNarrationFocusRef restored = PdfNarrationFocusMetadata.decode(
                PdfNarrationFocusMetadata.encode(source)).orElseThrow();

        assertEquals(source, restored);
    }

    @Test
    void rejectsMalformedMetadataWithoutBreakingPlayback() {
        assertTrue(PdfNarrationFocusMetadata.decode("broken").isEmpty());
    }
}
