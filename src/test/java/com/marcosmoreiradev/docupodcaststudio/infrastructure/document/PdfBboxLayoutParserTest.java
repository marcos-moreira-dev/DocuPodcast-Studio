package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PdfBboxLayoutParserTest {
    @Test
    void parsesPopplerBboxLayoutPagesBlocksAndWords() throws Exception {
        PdfBboxExtraction extraction = new PdfBboxLayoutParser().parse("""
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml">
                <body>
                <doc>
                  <page width="612.000000" height="792.000000">
                    <flow>
                      <block xMin="72.000000" yMin="63.384000" xMax="260.000000" yMax="91.000000">
                        <line xMin="72.000000" yMin="63.384000" xMax="250.000000" yMax="74.484000">
                          <word xMin="72.000000" yMin="63.384000" xMax="110.000000" yMax="74.484000">Solve</word>
                          <word xMin="114.000000" yMin="63.384000" xMax="130.000000" yMax="74.484000">f(x)=0</word>
                        </line>
                        <line xMin="72.000000" yMin="79.384000" xMax="180.000000" yMax="90.484000">
                          <word xMin="72.000000" yMin="79.384000" xMax="120.000000" yMax="90.484000">by</word>
                          <word xMin="124.000000" yMin="79.384000" xMax="180.000000" yMax="90.484000">Newton</word>
                        </line>
                      </block>
                    </flow>
                  </page>
                </doc>
                </body>
                </html>
                """);

        assertEquals(1, extraction.pageCount());
        PdfBboxPage page = extraction.pages().getFirst();
        assertEquals(612.0, page.width());
        assertEquals(792.0, page.height());
        PdfBboxTextBlock block = page.blocks().getFirst();
        assertEquals("Solve f(x)=0\nby Newton", block.text());
        assertEquals("72.000,63.384,260.000,91.000", block.bbox().compact());
        assertEquals("612.000", block.pageWidthLabel());
    }

    @Test
    void ordersAndMergesNearbyFragmentedBlocksByPdfCoordinates() throws Exception {
        PdfBboxExtraction extraction = new PdfBboxLayoutParser().parse("""
                <html xmlns="http://www.w3.org/1999/xhtml">
                <body>
                <doc>
                  <page width="612.000000" height="792.000000">
                    <flow>
                      <block xMin="72.000000" yMin="82.000000" xMax="180.000000" yMax="92.000000">
                        <line><word>Second</word><word>line</word></line>
                      </block>
                      <block xMin="72.000000" yMin="60.000000" xMax="170.000000" yMax="70.000000">
                        <line><word>First</word><word>line</word></line>
                      </block>
                      <block xMin="360.000000" yMin="86.000000" xMax="420.000000" yMax="96.000000">
                        <line><word>Side</word></line>
                      </block>
                    </flow>
                  </page>
                </doc>
                </body>
                </html>
                """);

        PdfBboxPage page = extraction.pages().getFirst();

        assertEquals(2, page.blocks().size());
        assertEquals("First line\nSecond line", page.blocks().getFirst().text());
        assertEquals("Side", page.blocks().get(1).text());
    }
}
