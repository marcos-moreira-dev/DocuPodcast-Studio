package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentOutlineHint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PdfBookmarkOutlineHintProviderTest {
    @Test
    void parsesSimpleUnencryptedPdfBookmarksMappedToPhysicalPages() {
        String raw = """
                %PDF-1.4
                1 0 obj
                << /Type /Catalog /Outlines 10 0 R /Pages 2 0 R >>
                endobj
                2 0 obj
                << /Type /Pages /Kids [3 0 R 4 0 R] /Count 2 >>
                endobj
                3 0 obj
                << /Type /Page /Parent 2 0 R >>
                endobj
                4 0 obj
                << /Type /Page /Parent 2 0 R >>
                endobj
                10 0 obj
                << /Type /Outlines /First 11 0 R /Count 2 >>
                endobj
                11 0 obj
                << /Title (Chapter 1) /Parent 10 0 R /Dest [3 0 R /Fit] /First 12 0 R >>
                endobj
                12 0 obj
                << /Title (1.1 Review of Calculus) /Parent 11 0 R /Dest [4 0 R /Fit] >>
                endobj
                %%EOF
                """;

        List<DocumentOutlineHint> hints = new PdfBookmarkOutlineHintProvider().parse(raw);

        assertEquals(2, hints.size());
        assertEquals("Chapter 1", hints.get(0).title());
        assertEquals(1, hints.get(0).level());
        assertEquals("1", hints.get(0).sourcePage());
        assertEquals("1.1 Review of Calculus", hints.get(1).title());
        assertEquals(2, hints.get(1).level());
        assertEquals("2", hints.get(1).sourcePage());
    }
}
