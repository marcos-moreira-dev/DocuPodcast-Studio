package com.marcosmoreiradev.docupodcaststudio.application.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfOcrTsvParserTest {
    @Test
    void parsesTesseractTsvIntoWordsLinesAndPdfPointBboxes() {
        String tsv = String.join(System.lineSeparator(),
                "level\tpage_num\tblock_num\tpar_num\tline_num\tword_num\tleft\ttop\twidth\theight\tconf\ttext",
                "5\t1\t1\t1\t1\t1\t100\t50\t200\t40\t92\tHola",
                "5\t1\t1\t1\t1\t2\t320\t50\t180\t40\t88\tmundo",
                "5\t1\t1\t1\t2\t1\t100\t160\t100\t30\t80\tLinea");

        PdfOcrPageResult result = new PdfOcrTsvParser().parse(tsv, 1, 216, 1000, 2000, 500.0, 1000.0);

        assertEquals(3, result.words().size());
        assertEquals(2, result.lines().size());
        assertEquals(PdfTextLayerOrigin.OCR_LOCAL, result.textLayer().origin());
        assertEquals("Hola mundo", result.lines().get(0).text());
        assertEquals(50.0, result.words().get(0).region().xMinPoints(), 0.001);
        assertEquals(25.0, result.words().get(0).region().yMinPoints(), 0.001);
        assertEquals(250.0, result.lines().get(0).region().xMaxPoints(), 0.001);
        assertEquals(45.0, result.lines().get(0).region().yMaxPoints(), 0.001);
        assertTrue(result.textLayer().available());
    }
}
