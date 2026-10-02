package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageRole;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfSemanticCoverageValidatorTest {
    private final PdfSemanticCoverageValidator validator =
            new PdfSemanticCoverageValidator();

    @Test
    void freezesTheEmpiricalAcceptanceBaseline() {
        assertEquals(0.84,
                PdfSemanticCoverageValidator.REQUIRED_TOKEN_COVERAGE, 0.0);
        assertEquals(0.58,
                PdfSemanticCoverageValidator.REQUIRED_BLOCK_COVERAGE, 0.0);
        assertEquals(0.80,
                PdfSemanticCoverageValidator.REQUIRED_SHORT_BLOCK_COVERAGE, 0.0);
    }
    private final PdfNativeTextQualityAssessor assessor =
            new PdfNativeTextQualityAssessor();

    @Test
    void acceptsPunctuationAndWhitespaceNormalizationWithoutLiteralDiff() {
        PdfTextLayer nativeLayer = layer(
                "Cobertura semántica del documento",
                "En cálculo algunos límites son importantes porque actúan como piezas estructurales.",
                "Este segundo párrafo conserva la explicación principal del ejemplo completo.");
        PdfSemanticPageAnalysis candidate = analysis(
                element(PdfRegionType.TITLE, 50, 50, 900, 110,
                        "Cobertura semantica del documento", ""),
                element(PdfRegionType.PARAGRAPH, 50, 140, 900, 350,
                        "En cálculo, algunos límites son importantes porque actúan como piezas estructurales.\nEste segundo párrafo conserva la explicación principal del ejemplo completo.", ""));

        var result = validator.validate(nativeLayer, assessor.assess(nativeLayer),
                candidate, false);

        assertEquals(PdfSemanticCoverageStatus.ACCEPTED, result.status());
        assertTrue(result.coveredTokenRatio() >= 0.84);
        assertTrue(result.missingEvidence().isEmpty());
    }

    @Test
    void detectsWholeMissingTitleAndLongParagraphAndRejectsAfterVerifier() {
        PdfTextLayer nativeLayer = layer(
                "Título principal que no debe desaparecer",
                "Primer párrafo suficientemente largo con información central para el lector.",
                "Segundo párrafo completamente omitido que contiene una conclusión material importante.");
        PdfSemanticPageAnalysis incomplete = analysis(
                element(PdfRegionType.PARAGRAPH, 50, 180, 900, 300,
                        "Primer párrafo suficientemente largo con información central para el lector.", ""));

        var primary = validator.validate(nativeLayer, assessor.assess(nativeLayer),
                incomplete, false);
        var verified = validator.validate(nativeLayer, assessor.assess(nativeLayer),
                incomplete, true);

        assertEquals(PdfSemanticCoverageStatus.NEEDS_VERIFICATION, primary.status());
        assertTrue(primary.missingEvidence().size() >= 2);
        assertEquals(PdfSemanticCoverageStatus.REJECTED, verified.status());
    }

    @Test
    void detectsShortHeadingEvenWhenCommonWordsAppearElsewhere() {
        PdfTextLayer nativeLayer = layer(
                "Hallazgos en una página",
                "La página contiene una tabla completa con resultados y evidencia experimental.",
                "El análisis en una computadora conserva todas las filas visibles de la tabla.");
        PdfSemanticPageAnalysis incomplete = analysis(
                element(PdfRegionType.PARAGRAPH, 50, 180, 900, 300,
                        "La página contiene una tabla completa con resultados y evidencia experimental. "
                                + "El análisis en una computadora conserva todas las filas visibles de la tabla.",
                        ""));

        var result = validator.validate(nativeLayer, assessor.assess(nativeLayer),
                incomplete, false);

        assertEquals(PdfSemanticCoverageStatus.NEEDS_VERIFICATION, result.status());
        assertTrue(result.missingEvidence().contains("Hallazgos en una página"));
    }

    @Test
    void lossyNarrowMathFragmentsDoNotRejectOtherwiseCompletePage() {
        PdfPageRegion prose = new PdfPageRegion(1, 50, 100, 550, 130,
                600, 800);
        PdfPageRegion formula = new PdfPageRegion(1, 250, 180, 350, 195,
                600, 800);
        PdfPageRegion limitSubscript = new PdfPageRegion(1, 270, 196, 330, 210,
                600, 800);
        PdfTextLayer nativeLayer = new PdfTextLayer(1,
                PdfTextLayerOrigin.NATIVE_BBOX, List.of(
                line("La demostracion aplica el teorema del encaje y obtiene el resultado final.", prose),
                line("cos x -> 1", formula),
                line("x->0+ x", limitSubscript)), List.of());
        PdfSemanticPageAnalysis candidate = analysis(element(
                PdfRegionType.PARAGRAPH, 50, 100, 900, 400,
                "La demostracion aplica el teorema del encaje y obtiene el resultado final. "
                        + "Cuando x tiende a 0 por la derecha, cos x tiende a 1.", ""));

        var result = validator.validate(nativeLayer, assessor.assess(nativeLayer),
                candidate, false);

        assertEquals(PdfSemanticCoverageStatus.ACCEPTED, result.status());
        assertTrue(result.missingEvidence().isEmpty());
    }

    @Test
    void p2OcrFormulaNoiseDoesNotBecomeMissingSignificantProse() {
        PdfPageRegion prose = new PdfPageRegion(2, 44, 68, 551, 90,
                595.276, 841.89);
        PdfPageRegion firstNoise = new PdfPageRegion(2, 248.672, 235.019,
                327.402, 248.702, 595.276, 841.89);
        PdfPageRegion secondNoise = new PdfPageRegion(2, 248.432, 243.901,
                347.804, 255.184, 595.276, 841.89);
        PdfPageRegion thirdNoise = new PdfPageRegion(2, 250.352, 529.332,
                356.205, 537.494, 595.276, 841.89);
        PdfTextLayer evidence = new PdfTextLayer(2, PdfTextLayerOrigin.OCR_LOCAL,
                List.of(
                        new PdfTextLine(2, "Las tres areas pueden escribirse con formulas elementales.",
                                prose, List.of(), 0.96),
                        new PdfTextLine(2, "1. el cta", firstNoise, List.of(), 0.5902),
                        new PdfTextLine(2, "5 Sing 5 9 an z.", secondNoise, List.of(), 0.7373),
                        new PdfTextLine(2, "-x zx a", thirdNoise, List.of(), 0.2188)),
                List.of());
        PdfSemanticPageAnalysis complete = analysis(
                element(PdfRegionType.PARAGRAPH, 0, 83, 1000, 115,
                        "Las tres areas pueden escribirse con formulas elementales.", ""),
                element(PdfRegionType.MATH, 0, 297, 1000, 314,
                        "1/2 sin x < 1/2 x < 1/2 tan x.", ""),
                element(PdfRegionType.MATH, 0, 717, 1000, 742,
                        "sin(-x)/-x = -sin x/-x = sin x/x.", ""));

        var result = validator.validate(evidence,
                new PdfNativeTextQualityReport(PdfNativeTextQuality.RELIABLE,
                        1.0, List.of()), complete, true);
        var audit = validator.audit(evidence, complete);

        assertEquals(PdfSemanticCoverageStatus.ACCEPTED, result.status());
        assertTrue(result.missingEvidence().isEmpty());
        assertEquals(PdfSemanticCoverageEvidenceAudit.Classification.MATH_FRAGMENT,
                audit.get(1).classification());
        assertEquals(PdfSemanticCoverageEvidenceAudit.Classification.MATH_FRAGMENT,
                audit.get(2).classification());
        assertEquals(PdfSemanticCoverageEvidenceAudit.Classification.OCR_NOISE,
                audit.get(3).classification());
        assertEquals("OCR_LINE", audit.get(3).source());
        assertTrue(audit.get(3).areaPoints() > 800.0);
        assertEquals(0L, audit.stream().filter(
                PdfSemanticCoverageEvidenceAudit::significantMissingContent).count());
    }

    @Test
    void highConfidenceOmittedBodyProseRemainsTrueMissingContent() {
        PdfPageRegion present = new PdfPageRegion(2, 50, 100, 550, 130,
                600, 800);
        PdfPageRegion omitted = new PdfPageRegion(2, 50, 300, 550, 340,
                600, 800);
        String kept = "El desarrollo conserva la primera explicacion completa del argumento.";
        String missing = "La conclusion material demuestra que el limite bilateral existe y vale uno.";
        PdfTextLayer evidence = new PdfTextLayer(2, PdfTextLayerOrigin.OCR_LOCAL,
                List.of(new PdfTextLine(2, kept, present, List.of(), 0.97),
                        new PdfTextLine(2, missing, omitted, List.of(), 0.96)), List.of());
        PdfSemanticPageAnalysis incomplete = analysis(element(
                PdfRegionType.PARAGRAPH, 50, 100, 920, 200, kept, ""));

        var result = validator.validate(evidence,
                new PdfNativeTextQualityReport(PdfNativeTextQuality.RELIABLE,
                        1.0, List.of()), incomplete, true);
        var decision = validator.audit(evidence, incomplete).get(1);

        assertEquals(PdfSemanticCoverageStatus.REJECTED, result.status());
        assertTrue(result.missingEvidence().contains(missing));
        assertEquals(PdfSemanticCoverageEvidenceAudit.Classification.TRUE_MISSING_CONTENT,
                decision.classification());
    }

    @Test
    void tableCellsObservedAsSeparateOcrLinesAreCoveredByCanonicalTableRegion() {
        PdfPageRegion firstRow = new PdfPageRegion(2, 80, 220, 520, 245,
                600, 800);
        PdfPageRegion secondRow = new PdfPageRegion(2, 80, 250, 520, 275,
                600, 800);
        PdfTextLayer evidence = new PdfTextLayer(2, PdfTextLayerOrigin.OCR_LOCAL,
                List.of(new PdfTextLine(2, "Triangulo interior OAB un medio seno x",
                                firstRow, List.of(), 0.92),
                        new PdfTextLine(2, "Sector circular OAB un medio x",
                                secondRow, List.of(), 0.91)), List.of());
        PdfSemanticPageAnalysis complete = analysis(element(PdfRegionType.TABLE,
                100, 240, 900, 380, """
                        Comparacion de areas
                        - Region: Triangulo interior OAB; Area: 1/2 sin x
                        - Region: Sector circular OAB; Area: 1/2 x
                        """, ""));

        var result = validator.validate(evidence,
                new PdfNativeTextQualityReport(PdfNativeTextQuality.RELIABLE,
                        1.0, List.of()), complete, true);
        var audit = validator.audit(evidence, complete);

        assertEquals(PdfSemanticCoverageStatus.ACCEPTED, result.status());
        assertTrue(result.missingEvidence().isEmpty());
        assertTrue(audit.stream().allMatch(item -> item.classification()
                == PdfSemanticCoverageEvidenceAudit.Classification
                .COVERED_DIFFERENT_REPRESENTATION));
        assertTrue(audit.stream().allMatch(item -> item.semanticRegionType()
                == PdfRegionType.TABLE));
    }

    @Test
    void ignoresShortRepeatedHeaderInsideOuterPageMargin() {
        PdfPageRegion headerBox = new PdfPageRegion(1, 50, 20, 400, 35,
                600, 800);
        PdfPageRegion bodyBox = new PdfPageRegion(1, 50, 100, 550, 140,
                600, 800);
        PdfTextLayer nativeLayer = new PdfTextLayer(1,
                PdfTextLayerOrigin.NATIVE_BBOX, List.of(
                new PdfTextLine(1, "Manual técnico del fabricante", headerBox,
                        List.of(new PdfTextToken("Manual técnico del fabricante",
                                headerBox, 1.0)), 1.0),
                new PdfTextLine(1,
                        "La potencia es la energía consumida durante un cierto tiempo.",
                        bodyBox, List.of(new PdfTextToken(
                        "La potencia es la energía consumida durante un cierto tiempo.",
                        bodyBox, 1.0)), 1.0)), List.of());
        PdfSemanticPageAnalysis candidate = analysis(element(
                PdfRegionType.PARAGRAPH, 50, 100, 900, 300,
                "La potencia es la energía consumida durante un cierto tiempo.", ""));

        var result = validator.validate(nativeLayer, assessor.assess(nativeLayer),
                candidate, false);

        assertEquals(PdfSemanticCoverageStatus.ACCEPTED, result.status());
        assertTrue(result.missingEvidence().isEmpty());
    }

    @Test
    void ignoresIsolatedEditorialFooterWithoutIgnoringBodyCoverage() {
        PdfPageRegion bodyBox = new PdfPageRegion(1, 50, 120, 550, 180,
                600, 800);
        PdfPageRegion footerBox = new PdfPageRegion(1, 70, 770, 530, 782,
                600, 800);
        String body = "La curva principal se aproxima al limite desde ambos lados.";
        String editorialFooter = "Documento preparado como muestra editorial de composicion academica; "
                + "las figuras vectoriales no requieren recursos externos adicionales.";
        PdfTextLayer nativeLayer = new PdfTextLayer(1,
                PdfTextLayerOrigin.NATIVE_BBOX, List.of(
                line(body, bodyBox),
                line(editorialFooter, footerBox)), List.of());
        PdfSemanticPageAnalysis candidate = analysis(element(
                PdfRegionType.PARAGRAPH, 80, 150, 920, 500, body, ""));

        var result = validator.validate(nativeLayer, assessor.assess(nativeLayer),
                candidate, false);

        assertEquals(PdfSemanticCoverageStatus.ACCEPTED, result.status());
        assertTrue(result.missingEvidence().isEmpty());
    }

    @Test
    void significantBodyLikeContentNearBottomMarginStillBlocksCoverage() {
        PdfPageRegion bodyBox = new PdfPageRegion(1, 50, 120, 550, 180,
                600, 800);
        PdfPageRegion conclusionBox = new PdfPageRegion(1, 50, 748, 550, 790,
                600, 800);
        String body = "El desarrollo compara las dos funciones principales del problema.";
        String conclusion = "Por consiguiente el limite bilateral de las dos funciones principales "
                + "existe y vale uno; esta conclusion completa el argumento del problema.";
        PdfTextLayer nativeLayer = new PdfTextLayer(1,
                PdfTextLayerOrigin.NATIVE_BBOX, List.of(
                line(body, bodyBox),
                line(conclusion, conclusionBox)), List.of());
        PdfSemanticPageAnalysis candidate = analysis(element(
                PdfRegionType.PARAGRAPH, 80, 150, 920, 500, body, ""));

        var result = validator.validate(nativeLayer, assessor.assess(nativeLayer),
                candidate, false);

        assertEquals(PdfSemanticCoverageStatus.NEEDS_VERIFICATION,
                result.status());
        assertTrue(result.missingEvidence().contains(conclusion));
    }

    @Test
    void significantImageWithoutVisualExplanationRejectsAfterVerifier() {
        PdfTextLayer unavailable = new PdfTextLayer(1,
                PdfTextLayerOrigin.UNAVAILABLE, List.of(), List.of());
        PdfSemanticPageAnalysis imageWithoutSpeech = analysis(element(
                PdfRegionType.IMAGE, 180, 100, 820, 500,
                "eje x, eje y, curva principal, hueco", ""));

        var result = validator.validate(unavailable,
                assessor.assess(unavailable), imageWithoutSpeech, true);

        assertEquals(PdfSemanticCoverageStatus.REJECTED, result.status());
        assertTrue(result.reasons().contains("imageMissingVisualExplanation"));
        assertTrue(result.reasons().contains("verifierDisagreement"));
    }

    @Test
    void noReliableTextRequiresOneVisualVerifierAndStillRejectsBadTypes() {
        PdfTextLayer unavailable = new PdfTextLayer(1,
                PdfTextLayerOrigin.UNAVAILABLE, List.of(), List.of());
        PdfNativeTextQualityReport quality = assessor.assess(unavailable);
        PdfSemanticPageAnalysis coherent = analysis(element(
                PdfRegionType.PARAGRAPH, 50, 100, 900, 300,
                "Una página escaneada con contenido visible coherente.", ""));
        PdfSemanticPageAnalysis suspicious = analysis(element(
                PdfRegionType.MATH, 50, 100, 900, 300,
                "Este párrafo largo es prosa ordinaria sin ninguna señal matemática observable y fue clasificado incorrectamente.", ""));

        assertEquals(PdfSemanticCoverageStatus.NEEDS_VERIFICATION,
                validator.validate(unavailable, quality, coherent, false).status());
        assertEquals(PdfSemanticCoverageStatus.ACCEPTED,
                validator.validate(unavailable, quality, coherent, true).status());
        assertEquals(PdfSemanticCoverageStatus.REJECTED,
                validator.validate(unavailable, quality, suspicious, true).status());
    }

    @Test
    void rejectsObviouslyNonContentRoleWithoutSilentlyReclassifyingIt() {
        String prose = "Esta pagina desarrolla una explicacion ordinaria con suficiente texto "
                + "continuo para demostrar que no es un indice ni un catalogo de entradas. ";
        PdfSemanticPageAnalysis candidate = new PdfSemanticPageAnalysis(
                0, "es", PdfPageRole.INDEX,
                List.of(element(PdfRegionType.PARAGRAPH, 50, 100, 900, 500,
                        prose.repeat(4), "")), 0.0, List.of());
        PdfTextLayer unavailable = new PdfTextLayer(1,
                PdfTextLayerOrigin.UNAVAILABLE, List.of(), List.of());

        var primary = validator.validate(unavailable,
                assessor.assess(unavailable), candidate, false);
        var verified = validator.validate(unavailable,
                assessor.assess(unavailable), candidate, true);

        assertEquals(PdfSemanticCoverageStatus.NEEDS_VERIFICATION,
                primary.status());
        assertTrue(primary.reasons().contains("suspiciousPageRole"));
        assertEquals(PdfSemanticCoverageStatus.REJECTED, verified.status());
        assertEquals(PdfPageRole.INDEX, candidate.pageRole());
    }

    @Test
    void rejectsImplausiblySparseIndexAfterVisualVerifier() {
        PdfSemanticPageAnalysis sparse = new PdfSemanticPageAnalysis(
                0, "en", PdfPageRole.INDEX,
                List.of(element(PdfRegionType.PARAGRAPH, 50, 100, 900, 200,
                        "Index", "")), 0.0, List.of());
        PdfTextLayer unavailable = new PdfTextLayer(1,
                PdfTextLayerOrigin.UNAVAILABLE, List.of(), List.of());

        var primary = validator.validate(unavailable,
                assessor.assess(unavailable), sparse, false);
        var verified = validator.validate(unavailable,
                assessor.assess(unavailable), sparse, true);

        assertEquals(PdfSemanticCoverageStatus.NEEDS_VERIFICATION,
                primary.status());
        assertEquals(PdfSemanticCoverageStatus.REJECTED, verified.status());
        assertTrue(verified.reasons().contains("suspiciousSparseStructuredPage"));
    }

    @Test
    void rejectsBlankPageDescriptionAsNonSubstantiveContent() {
        PdfSemanticPageAnalysis blank = analysis(element(
                PdfRegionType.IMAGE, 0, 0, 1000, 1000, "",
                "La imagen muestra una página en blanco, sin contenido textual visible."));
        PdfTextLayer unavailable = new PdfTextLayer(1,
                PdfTextLayerOrigin.UNAVAILABLE, List.of(), List.of());

        var verified = validator.validate(unavailable,
                assessor.assess(unavailable), blank, true);

        assertEquals(PdfSemanticCoverageStatus.REJECTED, verified.status());
        assertTrue(verified.reasons().contains("noSubstantiveVisibleContent"));
    }

    @Test
    void acceptsCompleteTableExpressedAsLabeledRows() {
        String table = """
                Tabla de hallazgos
                - Hallazgo: La resolucion conserva la informacion.
                  Evidencia experimental: La pagina se leyo completa.
                  Implicacion: El resultado es util para lectura.
                - Hallazgo: La salida depende del contenido.
                  Evidencia experimental: Se conservaron todas las celdas.
                  Implicacion: No se pierde la tabla visible.
                """;
        PdfSemanticPageAnalysis candidate = analysis(element(
                PdfRegionType.TABLE, 50, 100, 900, 700, table, ""));
        PdfTextLayer unavailable = new PdfTextLayer(1,
                PdfTextLayerOrigin.UNAVAILABLE, List.of(), List.of());

        var result = validator.validate(unavailable,
                assessor.assess(unavailable), candidate, true);

        assertEquals(PdfSemanticCoverageStatus.ACCEPTED, result.status());
        assertTrue(!result.reasons().contains("tableMissingVisibleCells"));
    }

    private static PdfTextLayer layer(String... values) {
        java.util.ArrayList<PdfTextLine> lines = new java.util.ArrayList<>();
        double y = 80;
        for (String value : values) {
            PdfPageRegion region = new PdfPageRegion(1, 50, y, 550,
                    y + 24, 600, 800);
            lines.add(new PdfTextLine(1, value, region,
                    List.of(new PdfTextToken(value, region, 1.0)), 1.0));
            y += 80;
        }
        return new PdfTextLayer(1, PdfTextLayerOrigin.NATIVE_BBOX,
                lines, List.of());
    }

    private static PdfTextLine line(String text, PdfPageRegion region) {
        return new PdfTextLine(1, text, region,
                List.of(new PdfTextToken(text, region, 1.0)), 1.0);
    }

    private static PdfSemanticPageAnalysis analysis(
            PdfSemanticPageAnalysis.Element... elements) {
        return new PdfSemanticPageAnalysis(0, "es", PdfPageRole.CONTENT,
                List.of(elements), 0.0, List.of());
    }

    private static PdfSemanticPageAnalysis.Element element(
            PdfRegionType type, double x1, double y1, double x2, double y2,
            String source, String speech) {
        return new PdfSemanticPageAnalysis.Element("", 0, type,
                new PdfSemanticPageAnalysis.NormalizedBox(x1, y1, x2, y2),
                source, speech, PdfNarratability.NARRATABLE,
                0.0, List.of(), Map.of());
    }
}
