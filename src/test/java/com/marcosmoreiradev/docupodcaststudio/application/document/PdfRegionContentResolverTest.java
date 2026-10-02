package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageRole;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class PdfRegionContentResolverTest {
    @TempDir java.nio.file.Path temp;
    private final PdfRegionContentResolver resolver = new PdfRegionContentResolver();

    @Test
    void cleanLiteralCropFillsTheSameRegionWithoutAVlmContentCall() {
        var result = resolver.resolve(page("R2"), layer(
                "Texto literal limpio obtenido dentro del recorte.", 0.94));

        assertEquals("R2", result.analysis().elements().getFirst().responseId());
        assertEquals(1, result.literalRegions());
        assertEquals("NATIVE_TEXT_CROP", result.analysis().elements().getFirst()
                .attributes().get("contentResolver"));
        assertEquals("true", result.analysis().elements().getFirst()
                .attributes().get("contentCallSaved"));
        assertFalse(result.analysis().elements().getFirst().attributes()
                .get("lineBboxes").isBlank());
    }

    @Test
    void broadPrimaryBoxUsesOnlyNativeLinesGroundedInItsSource() {
        PdfPageRegion title = new PdfPageRegion(1, 44, 40, 420, 51, 600, 800);
        PdfPageRegion foreignFormula = new PdfPageRegion(1, 228, 48, 265, 56, 600, 800);
        PdfTextLayer layer = new PdfTextLayer(1, PdfTextLayerOrigin.NATIVE_BBOX,
                List.of(
                        new PdfTextLine(1, "Cuando una fracción se vuelve certeza", title, List.of(), 0.98),
                        new PdfTextLine(1, "sin x", foreignFormula, List.of(), 0.98)), List.of());

        var result = resolver.resolve(page("R2",
                "Cuando una fracción se vuelve certeza"), layer);
        var resolved = result.analysis().elements().getFirst();

        assertEquals("Cuando una fracción se vuelve certeza", resolved.sourceText());
        assertEquals("44.000,40.000,420.000,51.000",
                resolved.attributes().get("playbackBboxes"));
        assertEquals("NATIVE_TEXT_LINES",
                resolved.attributes().get("playbackGeometryOrigin"));
    }

    @Test
    void contaminatedTitleSourceCannotClaimALineOwnedByTheNeighbouringSubheading() {
        PdfPageRegion titleLine = new PdfPageRegion(1, 100, 100, 550, 120, 600, 800);
        PdfPageRegion neighbourLine = new PdfPageRegion(1, 100, 150, 420, 170, 600, 800);
        PdfSemanticPageAnalysis.Element title = element("TITLE", PdfRegionType.TITLE,
                new PdfSemanticPageAnalysis.NormalizedBox(140, 100, 940, 230),
                "Informe de resultados sin x");
        PdfSemanticPageAnalysis.Element subheading = element("SUB", PdfRegionType.SUBHEADING,
                new PdfSemanticPageAnalysis.NormalizedBox(140, 180, 760, 240),
                "sin x explica el limite");
        PdfTextLayer layer = new PdfTextLayer(1, PdfTextLayerOrigin.NATIVE_BBOX,
                List.of(new PdfTextLine(1, "Informe de resultados", titleLine, List.of(), 0.99),
                        new PdfTextLine(1, "sin x", neighbourLine, List.of(), 0.99)), List.of());

        var result = resolver.resolve(new PdfSemanticPageAnalysis(1, "es",
                PdfPageRole.CONTENT, List.of(title, subheading), 1.0, List.of()), layer);
        var resolvedTitle = result.analysis().elements().getFirst();

        assertEquals("100.000,100.000,550.000,120.000",
                resolvedTitle.attributes().get("playbackBboxes"));
        assertEquals("single-confident-line-within-semantic-region",
                resolvedTitle.attributes().get("playbackGeometryReason"));
    }

    @Test
    void sharedTokensNeverMoveTitlePlaybackToTheNeighbouringLine() {
        PdfPageRegion titleLine = new PdfPageRegion(1, 100, 100, 550, 120, 600, 800);
        PdfPageRegion neighbourLine = new PdfPageRegion(1, 100, 150, 500, 170, 600, 800);
        PdfSemanticPageAnalysis.Element title = element("TITLE", PdfRegionType.TITLE,
                new PdfSemanticPageAnalysis.NormalizedBox(140, 100, 940, 230),
                "Informe anual de resultados");
        PdfSemanticPageAnalysis.Element subheading = element("SUB", PdfRegionType.SUBHEADING,
                new PdfSemanticPageAnalysis.NormalizedBox(140, 180, 900, 240),
                "Informe anual resumido para estudiantes");
        PdfTextLayer layer = new PdfTextLayer(1, PdfTextLayerOrigin.NATIVE_BBOX,
                List.of(new PdfTextLine(1, "Informe anual de resultados", titleLine, List.of(), 0.99),
                        new PdfTextLine(1, "Informe anual resumido", neighbourLine, List.of(), 0.99)), List.of());

        var result = resolver.resolve(new PdfSemanticPageAnalysis(1, "es",
                PdfPageRole.CONTENT, List.of(title, subheading), 1.0, List.of()), layer);

        assertEquals("100.000,100.000,550.000,120.000", result.analysis()
                .elements().getFirst().attributes().get("playbackBboxes"));
    }

    @Test
    void trustworthyLineKeepsItsNaturalWidthInsteadOfExpandingToSemanticBox() {
        PdfPageRegion exactLine = new PdfPageRegion(1, 100, 100, 550, 120, 1000, 800);
        PdfSemanticPageAnalysis.Element paragraph = element("P", PdfRegionType.PARAGRAPH,
                new PdfSemanticPageAnalysis.NormalizedBox(90, 90, 900, 140),
                "La linea real termina aqui");
        PdfTextLayer layer = new PdfTextLayer(1, PdfTextLayerOrigin.NATIVE_BBOX,
                List.of(new PdfTextLine(1, "La linea real termina aqui",
                        exactLine, List.of(), 0.99)), List.of());

        var result = resolver.resolve(new PdfSemanticPageAnalysis(1, "es",
                PdfPageRole.CONTENT, List.of(paragraph), 1.0, List.of()), layer);

        assertEquals("100.000,100.000,550.000,120.000", result.analysis()
                .elements().getFirst().attributes().get("playbackBboxes"));
    }

    @Test
    void loneVariableCannotGroundAParagraphBySubstringAlone() {
        PdfPageRegion foreignVariable = new PdfPageRegion(1, 400, 190, 410, 198, 600, 800);
        PdfSemanticPageAnalysis.Element paragraph = element("P", PdfRegionType.PARAGRAPH,
                new PdfSemanticPageAnalysis.NormalizedBox(50, 50, 950, 300),
                "El seno de x se aproxima al angulo");
        PdfTextLayer layer = new PdfTextLayer(1, PdfTextLayerOrigin.NATIVE_BBOX,
                List.of(new PdfTextLine(1, "x", foreignVariable, List.of(), 0.99)), List.of());

        var result = resolver.resolve(new PdfSemanticPageAnalysis(1, "es",
                PdfPageRole.CONTENT, List.of(paragraph), 1.0, List.of()), layer);

        assertEquals(null, result.analysis().elements().getFirst()
                .attributes().get("playbackBboxes"));
        assertEquals("semantic-fallback:no-confident-text-match", result.analysis()
                .elements().getFirst().attributes().get("playbackGeometryReason"));
    }

    @Test
    void mathKeepsCanonicalVlmGeometryEvenWhenTextLinesOverlap() {
        PdfSemanticPageAnalysis.Element mixed = new PdfSemanticPageAnalysis.Element(
                "M1", 0, PdfRegionType.MATH,
                new PdfSemanticPageAnalysis.NormalizedBox(50, 50, 950, 300),
                "La desigualdad seno de x es menor que x", "",
                PdfNarratability.NON_NARRATABLE, 0.9, List.of(),
                Map.of("contentRoute", "VLM_MIXED"));
        PdfPageRegion lineBox = new PdfPageRegion(1, 180, 90, 390, 104, 600, 800);

        var result = resolver.resolve(new PdfSemanticPageAnalysis(1, "es",
                        PdfPageRole.CONTENT, List.of(mixed), 1.0, List.of()),
                new PdfTextLayer(1, PdfTextLayerOrigin.NATIVE_BBOX,
                        List.of(new PdfTextLine(1, "desigualdad seno de x",
                                lineBox, List.of(), 0.97)), List.of()));

        assertEquals(mixed.sourceText(), result.analysis().elements().getFirst().sourceText());
        assertEquals(null, result.analysis().elements().getFirst()
                .attributes().get("playbackBboxes"));
        assertEquals(0, result.literalRegions());
    }

    @Test
    void rasterOcrGeometryGroundsTextWithoutChangingItsSemanticIdentity() {
        PdfSemanticPageAnalysis.Element title = element("TITLE", PdfRegionType.TITLE,
                new PdfSemanticPageAnalysis.NormalizedBox(60, 80, 940, 180),
                "Una geometria fisica precisa");
        PdfPageRegion ocrBox = new PdfPageRegion(1, 52, 72, 565, 128, 600, 800);
        PdfTextLayer raster = new PdfTextLayer(1, PdfTextLayerOrigin.OCR_LOCAL,
                List.of(new PdfTextLine(1, "Una geometria fisica precisa",
                        ocrBox, List.of(), 0.97)), List.of());
        PdfTextGeometryMap geometry = new PdfTextGeometryMap(1, raster, List.of(),
                "OCR_RASTER_PAGE", List.of());

        var result = resolver.resolve(new PdfSemanticPageAnalysis(1, "es",
                        PdfPageRole.CONTENT, List.of(title), 1.0, List.of()),
                new PdfTextLayer(1, PdfTextLayerOrigin.UNAVAILABLE, List.of(), List.of()),
                geometry, null, 600, 800, null, 1);
        var resolved = result.analysis().elements().getFirst();

        assertEquals("TITLE", resolved.responseId());
        assertEquals(title.box(), resolved.box());
        assertEquals("52.000,72.000,565.000,128.000",
                resolved.attributes().get("textGeometryBboxes"));
        assertEquals("OCR_RASTER_PAGE",
                resolved.attributes().get("playbackGeometryOrigin"));
    }

    @Test
    void rasterWordsDefineTightTextXmaxInsteadOfTheSemanticOrLineBox() {
        PdfSemanticPageAnalysis.Element paragraph = new PdfSemanticPageAnalysis.Element(
                "P", 0, PdfRegionType.PARAGRAPH,
                new PdfSemanticPageAnalysis.NormalizedBox(50, 80, 950, 300),
                "Una linea corta", "", PdfNarratability.NARRATABLE, 0.9,
                List.of(), Map.of("contentRoute", "OCR_SAFE", "playbackTarget", "true"));
        PdfPageRegion lineBox = new PdfPageRegion(1, 30, 70, 570, 140, 600, 800);
        List<PdfTextToken> words = List.of(
                new PdfTextToken("Una", new PdfPageRegion(1, 60, 90, 90, 108, 600, 800), 0.96),
                new PdfTextToken("linea", new PdfPageRegion(1, 96, 90, 145, 108, 600, 800), 0.97),
                new PdfTextToken("corta", new PdfPageRegion(1, 151, 90, 198, 108, 600, 800), 0.95));
        PdfTextLayer raster = new PdfTextLayer(1, PdfTextLayerOrigin.OCR_LOCAL,
                List.of(new PdfTextLine(1, "Una linea corta", lineBox,
                        words, 0.96)), List.of());

        var resolved = resolver.resolve(new PdfSemanticPageAnalysis(1, "es",
                        PdfPageRole.CONTENT, List.of(paragraph), 1.0, List.of()),
                new PdfTextLayer(1, PdfTextLayerOrigin.UNAVAILABLE, List.of(), List.of()),
                new PdfTextGeometryMap(1, raster, List.of(), "OCR_RASTER_PAGE", List.of()),
                null, 600, 800, null, 1).analysis().elements().getFirst();

        assertEquals("60.000,90.000,198.000,108.000",
                resolved.attributes().get(PdfTextVisualBoundsResolver.TIGHT_BBOX_ATTRIBUTE));
        assertEquals("60.000,90.000,198.000,108.000",
                resolved.attributes().get("playbackBboxes"));
        assertEquals("3", resolved.attributes().get(
                PdfTextVisualBoundsResolver.OCR_WORD_COUNT_ATTRIBUTE));
        assertEquals(paragraph.box(), resolved.box());
    }

    @Test
    void visualAndStructuredObjectsDoNotCollapseToInternalTextLines() {
        for (PdfRegionType type : List.of(PdfRegionType.IMAGE,
                PdfRegionType.TABLE, PdfRegionType.CODE)) {
            PdfSemanticPageAnalysis.Element object = new PdfSemanticPageAnalysis.Element(
                    type.name(), 0, type,
                    new PdfSemanticPageAnalysis.NormalizedBox(50, 50, 950, 300),
                    "etiqueta interna", "", PdfNarratability.NON_NARRATABLE,
                    0.9, List.of(), Map.of("contentRoute", "VLM_STRUCTURED"));

            var result = resolver.resolve(new PdfSemanticPageAnalysis(1, "es",
                            PdfPageRole.CONTENT, List.of(object), 1.0, List.of()),
                    layer("etiqueta interna", 0.98));

            assertEquals(null, result.analysis().elements().getFirst()
                    .attributes().get("playbackBboxes"));
        }
    }

    @Test
    void unusableLiteralCropPromotesTheSameRegionToVlm() {
        var result = resolver.resolve(page("R2"), layer("\uFFFD \uFFFD x", 0.2));

        assertEquals("R2", result.analysis().elements().getFirst().responseId());
        assertEquals(1, result.promotedToVlmRegions());
        assertEquals("VLM_MIXED", result.analysis().elements().getFirst()
                .attributes().get("contentRoute"));
        assertEquals("VLM_FALLBACK", result.analysis().elements().getFirst()
                .attributes().get("contentResolver"));
    }

    @Test
    void missingNativeTextUsesTesseractOnlyOnTheExistingRegionCrop()
            throws Exception {
        java.nio.file.Path source = temp.resolve("source.pdf");
        java.nio.file.Files.writeString(source, "%PDF-1.4");
        java.util.concurrent.atomic.AtomicReference<PdfOcrRequest> request =
                new java.util.concurrent.atomic.AtomicReference<>();
        PdfOcrEngine engine = value -> {
            request.set(value);
            PdfPageRegion box = value.cropRegion();
            PdfOcrLine line = new PdfOcrLine(value.pageNumber(),
                    "Texto limpio reconocido solamente en el recorte.", box,
                    List.of(), 0.96);
            PdfTextLayer layer = new PdfTextLayer(value.pageNumber(),
                    PdfTextLayerOrigin.OCR_LOCAL,
                    List.of(line.toTextLine()), List.of());
            return new PdfOcrPageResult(value.pageNumber(), value.dpi(),
                    400, 200, 600, 800, List.of(line), List.of(), layer,
                    List.of());
        };
        var result = new PdfRegionContentResolver(engine).resolve(page("R2"),
                new PdfTextLayer(2, PdfTextLayerOrigin.UNAVAILABLE, List.of(),
                        List.of()),
                new PreparedPdfWorkspaceRef(temp, source, "a".repeat(64)),
                600, 800, temp.resolve("ocr-cache"), 2);

        assertEquals("R2", result.analysis().elements().getFirst().responseId());
        assertEquals("TESSERACT_CROP", result.analysis().elements().getFirst()
                .attributes().get("contentResolver"));
        assertEquals(2, request.get().pageNumber());
        assertEquals(30.0, request.get().cropRegion().xMinPoints());
        assertEquals(240.0, request.get().cropRegion().yMaxPoints());
    }

    private static PdfSemanticPageAnalysis page(String id) {
        return page(id, "VLM provisional");
    }

    private static PdfSemanticPageAnalysis page(String id, String source) {
        var element = new PdfSemanticPageAnalysis.Element(id, 2,
                PdfRegionType.PARAGRAPH,
                new PdfSemanticPageAnalysis.NormalizedBox(50, 50, 950, 300),
                source, "", PdfNarratability.NARRATABLE, 0.9,
                List.of(), Map.of("contentRoute", "OCR_SAFE"));
        return new PdfSemanticPageAnalysis(1, "es", PdfPageRole.CONTENT,
                List.of(element), 1.0, List.of());
    }

    private static PdfTextLayer layer(String text, double confidence) {
        PdfPageRegion box = new PdfPageRegion(1, 60, 70, 550, 120, 600, 800);
        return new PdfTextLayer(1, PdfTextLayerOrigin.NATIVE_BBOX,
                List.of(new PdfTextLine(1, text, box, List.of(), confidence)),
                List.of());
    }

    private static PdfSemanticPageAnalysis.Element element(
            String id, PdfRegionType type,
            PdfSemanticPageAnalysis.NormalizedBox box, String source) {
        return new PdfSemanticPageAnalysis.Element(id, 0, type, box, source, "",
                PdfNarratability.NARRATABLE, 0.9, List.of(),
                Map.of("contentRoute", "VLM_MIXED", "playbackTarget", "true"));
    }
}
