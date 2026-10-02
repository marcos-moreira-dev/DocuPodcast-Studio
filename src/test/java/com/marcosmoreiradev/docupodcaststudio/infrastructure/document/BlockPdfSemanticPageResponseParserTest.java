package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfSemanticProtocolException;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BlockPdfSemanticPageResponseParserTest {
    private final BlockPdfSemanticPageResponseParser parser =
            new BlockPdfSemanticPageResponseParser();

    @Test
    void freezesTheExplicitSemanticAliasVocabulary() {
        var aliases = BlockPdfSemanticPageResponseParser.semanticTypeAliases();

        assertEquals(16, aliases.size());
        assertEquals(PdfRegionType.SIDEBAR, aliases.get("DEFINITION"));
        assertEquals(PdfRegionType.HEADING, aliases.get("SECTION_TITLE"));
        assertEquals(PdfRegionType.SUBHEADING,
                aliases.get("SUBSECTION_TITLE"));
        assertEquals(PdfRegionType.IMAGE, aliases.get("FIGURE"));
    }

    @Test
    void preservesArbitrarySourceAndUsesPhysicalEmissionOrder() throws Exception {
        var page = parser.parse("""
                PAGE|V1|es|CONTENT
                BEGIN|CODE|10|20|990|420|N|96
                SOURCE
                const data = {"items":[1, 2], "formula":"x^{2} + y_[0]"};
                  BEGIN|esto no es control
                END
                BEGIN|TABLE|10|450|990|900|N|94
                SOURCE
                Concepto | Enero | Febrero
                Ventas | 1250 | 1430
                Costos | 710 | 780
                END
                DONE
                """);

        assertEquals(2, page.elements().size());
        assertEquals(0, page.elements().get(0).readingOrder());
        assertEquals(PdfRegionType.CODE, page.elements().get(0).type());
        assertTrue(page.elements().get(0).sourceText().contains("{\"items\":[1, 2]"));
        assertTrue(page.elements().get(0).sourceText().contains("BEGIN|esto no es control"));
        assertEquals(0.0, page.elements().get(0).confidence(), 0.0);
        assertEquals("not-reported", page.elements().get(0).attributes()
                .get("modelConfidence"));
        assertEquals("Concepto | Enero | Febrero\nVentas | 1250 | 1430\nCostos | 710 | 780",
                page.elements().get(1).sourceText());
    }

    @Test
    void speechIsOnlyAControlMarkerForMathAndImage() throws Exception {
        var page = parser.parse("""
                PAGE|V1|es|CONTENT
                BEGIN|PARAGRAPH|0|0|1000|200|N|90
                SOURCE
                SOURCE
                SPEECH
                texto ordinario
                END
                BEGIN|MATH|0|210|1000|400|N|92
                SOURCE
                E = mc^2
                SPEECH
                E es igual a eme por ce al cuadrado.
                END
                DONE
                """);

        assertEquals("SOURCE\nSPEECH\ntexto ordinario",
                page.elements().get(0).sourceText());
        assertEquals("", page.elements().get(0).narrationText());
        assertEquals("E = mc^2", page.elements().get(1).sourceText());
        assertEquals("E es igual a eme por ce al cuadrado.",
                page.elements().get(1).narrationText());
    }

    @Test
    void missingDoneIsIncompleteAndNeverAValidPartialPage() {
        PdfSemanticProtocolException failure = assertThrows(
                PdfSemanticProtocolException.class, () -> parser.parse("""
                        PAGE|V1|es|CONTENT
                        BEGIN|PARAGRAPH|0|0|1000|200|N|90
                        SOURCE
                        Texto visible.
                        END
                        """));

        assertEquals(PdfSemanticProtocolException.Kind.INCOMPLETE, failure.kind());
    }

    @Test
    void rejectsP2VerifierShapeThatOmitsSourceAfterEveryBegin() {
        PdfSemanticProtocolException failure = assertThrows(
                PdfSemanticProtocolException.class, () -> parser.parse("""
                        PAGE|V1|es|CONTENT
                        BEGIN|PARAGRAPH|0|82|976|105|U|98
                        Las tres areas pueden escribirse con formulas elementales.
                        END
                        BEGIN|TABLE|132|120|857|249|U|98
                        | Region | Area | Razon geometrica |
                        | Triangulo interior OAB | 1/2 sin x | Base y altura |
                        END
                        DONE
                        """));

        assertEquals(PdfSemanticProtocolException.Kind.INVALID, failure.kind());
        assertEquals("La region 1 no comienza su payload con SOURCE.",
                failure.getMessage());
    }

    @Test
    void repairsOnlyNearBoundaryGeometry() throws Exception {
        var repaired = parser.parse("""
                PAGE|V1|es|CONTENT
                BEGIN|PARAGRAPH|-1|20|1001|200|N|90
                SOURCE
                Texto visible.
                END
                DONE
                """);

        assertEquals(0.0, repaired.elements().getFirst().box().xMin());
        assertEquals(1000.0, repaired.elements().getFirst().box().xMax());
        assertTrue(repaired.elements().getFirst().uncertainties()
                .contains("bbox-repaired-near-canonical-boundary"));

        PdfSemanticProtocolException invalid = assertThrows(
                PdfSemanticProtocolException.class, () -> parser.parse("""
                        PAGE|V1|es|CONTENT
                        BEGIN|PARAGRAPH|-20|20|1000|200|N|90
                        SOURCE
                        Texto visible.
                        END
                        DONE
                        """));
        assertEquals(PdfSemanticProtocolException.Kind.INVALID, invalid.kind());
    }

    @Test
    void rejectsMalformedOrEmptyRegions() {
        PdfSemanticProtocolException malformed = assertThrows(
                PdfSemanticProtocolException.class, () -> parser.parse("""
                        PAGE|V1|es|CONTENT
                        BEGIN|TABLE|0|0|1000|500|N|90
                        SOURCE
                        END
                        DONE
                        """));
        assertEquals(PdfSemanticProtocolException.Kind.INVALID, malformed.kind());
    }

    @Test
    void acceptsCompleteEmptyVerificationEnvelope() throws Exception {
        var verification = parser.parse("""
                PAGE|V1|es|CONTENT
                DONE
                """);

        assertTrue(verification.elements().isEmpty());
    }

    @Test
    void normalizesVisualBoxAliasToSidebarWithoutChangingItsText()
            throws Exception {
        var page = parser.parse("""
                PAGE|V1|es|CONTENT
                BEGIN|BOX|100|600|850|770|N|95
                SOURCE
                Resultado final
                END
                DONE
                """);

        assertEquals(PdfRegionType.SIDEBAR,
                page.elements().getFirst().type());
        assertEquals("Resultado final",
                page.elements().getFirst().sourceText());
    }

    @Test
    void normalizesObservedSubtitleAndNarrowFigureCaptionAliases()
            throws Exception {
        var page = parser.parse("""
                PAGE|V1|es|CONTENT
                BEGIN|SUBTITLE|50|65|950|85|N|95
                SOURCE
                La demostración geométrica
                END
                BEGIN|IMAGE|300|275|650|435|X|90
                SOURCE
                sector circular
                END
                BEGIN|FIGURE|180|435|930|455|N|90
                SOURCE
                Figura 1. El triángulo interior.
                END
                DONE
                """);

        assertEquals(PdfRegionType.SUBHEADING,
                page.elements().get(0).type());
        assertEquals(PdfRegionType.IMAGE, page.elements().get(1).type());
        assertEquals(PdfRegionType.CAPTION, page.elements().get(2).type());
        assertEquals("FIGURE", page.elements().get(2).attributes()
                .get("semanticTypeAlias"));
    }

    @Test
    void normalizesObservedHeadlineAliasToHeading() throws Exception {
        var page = parser.parse("""
                PAGE|V1|en|CONTENT
                BEGIN|HEADLINE|50|65|950|100|N|95
                SOURCE
                Memory architecture
                END
                DONE
                """);

        assertEquals(PdfRegionType.HEADING,
                page.elements().getFirst().type());
        assertEquals("HEADLINE", page.elements().getFirst().attributes()
                .get("semanticTypeAlias"));
    }

    @Test
    void normalizesObservedSectionTitleAliasToHeading() throws Exception {
        var page = parser.parse("""
                PAGE|V1|en|CONTENT
                BEGIN|SECTION_TITLE|50|65|950|100|N|95
                SOURCE
                Boundary-value problems
                END
                DONE
                """);

        assertEquals(PdfRegionType.HEADING,
                page.elements().getFirst().type());
        assertEquals("SECTION_TITLE", page.elements().getFirst().attributes()
                .get("semanticTypeAlias"));
    }

    @Test
    void normalizesObservedSubsectionTitleAliasToSubheading() throws Exception {
        var page = parser.parse("""
                PAGE|V1|en|CONTENT
                BEGIN|SUBSECTION_TITLE|50|105|950|140|N|95
                SOURCE
                Finite differences
                END
                DONE
                """);

        assertEquals(PdfRegionType.SUBHEADING,
                page.elements().getFirst().type());
        assertEquals("SUBSECTION_TITLE", page.elements().getFirst().attributes()
                .get("semanticTypeAlias"));
    }

    @Test
    void normalizesObservedDefinitionAliasToSidebar() throws Exception {
        var page = parser.parse("""
                PAGE|V1|en|CONTENT
                BEGIN|DEFINITION|180|430|920|620|N|95
                SOURCE
                Definition 1.1 A function has the limit L at x zero.
                END
                DONE
                """);

        assertEquals(PdfRegionType.SIDEBAR,
                page.elements().getFirst().type());
        assertEquals("DEFINITION", page.elements().getFirst().attributes()
                .get("semanticTypeAlias"));
    }

    @Test
    void normalizesObservedTextAliasToParagraph() throws Exception {
        var page = parser.parse("""
                PAGE|V1|en|CONTENT
                BEGIN|TEXT|50|145|950|180|N|95
                SOURCE
                Graduate Texts in Mathematics
                END
                DONE
                """);

        assertEquals(PdfRegionType.PARAGRAPH,
                page.elements().getFirst().type());
        assertEquals("TEXT", page.elements().getFirst().attributes()
                .get("semanticTypeAlias"));
    }

    @Test
    void keepsFigureAliasAsImageWhenItIsNotCaptionLike() throws Exception {
        var page = parser.parse("""
                PAGE|V1|es|CONTENT
                BEGIN|FIGURE|100|100|900|700|X|90
                SOURCE
                eje x, eje y, curva principal
                END
                DONE
                """);

        assertEquals(PdfRegionType.IMAGE,
                page.elements().getFirst().type());
    }
}
