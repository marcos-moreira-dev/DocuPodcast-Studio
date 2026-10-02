package com.marcosmoreiradev.docupodcaststudio.acceptance;

import com.marcosmoreiradev.docupodcaststudio.application.document.*;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageRole;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPreparedPdfDocumentRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.PdfBoxNativeTextEvidenceExtractor;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.PdfBoxRenderEngine;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.TesseractPdfOcrEngine;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.process.DefaultExternalProcessRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Physical P1/P2/P3 gate for raster OCR playback geometry; Qwen is never called. */
@EnabledIfSystemProperty(named = "docupodcast.pdf.ocrPlaybackGrounding", matches = "true")
final class PdfOcrPlaybackGroundingPhysicalTest {
    private static final List<PdfRegionType> TEXTUAL = List.of(
            PdfRegionType.TITLE, PdfRegionType.HEADING, PdfRegionType.SUBHEADING,
            PdfRegionType.PARAGRAPH, PdfRegionType.LIST, PdfRegionType.CAPTION,
            PdfRegionType.SIDEBAR, PdfRegionType.HEADER, PdfRegionType.FOOTER,
            PdfRegionType.PAGE_NUMBER);

    @Test
    void rasterOcrIsGeometricAuthorityForConfidentTextualRegions() throws Exception {
        Path pdf = Path.of(System.getProperty("docupodcast.pdf.grounding.source",
                "D:/Proyectos/Demostracion_limite_notable.pdf"));
        Path executable = Path.of(System.getProperty("docupodcast.tesseract.executable",
                "tools/tesseract/bin/tesseract.exe")).toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(pdf), pdf.toString());
        assertTrue(Files.isRegularFile(executable), executable.toString());

        var renderer = new PdfBoxRenderEngine();
        var ocr = new TesseractPdfOcrEngine(renderer,
                new DefaultExternalProcessRunner(), executable::toString);
        var nativeExtractor = new PdfBoxNativeTextEvidenceExtractor();
        var repository = new JsonPreparedPdfDocumentRepository();
        var resolver = new PdfRegionContentResolver();
        Path cache = Path.of("target/pdf-ocr-playback-grounding/cache");
        ArrayList<String> report = new ArrayList<>();
        int confident = 0;
        int misaligned = 0;
        boolean sawTitle = false;
        boolean sawShortLine = false;
        boolean sawMultiline = false;
        boolean sawCaption = false;
        boolean sawSidebar = false;

        for (int pageNumber = 1; pageNumber <= 3; pageNumber++) {
            Path workspace = canonicalWorkspace(pageNumber);
            var page = repository.loadPage(workspace, pageNumber).orElseThrow();
            PdfOcrPageResult ocrPage = ocr.recognize(new PdfOcrRequest(
                    pdf, pageNumber, 300, PdfOcrRequest.DEFAULT_MAX_PIXEL_COUNT,
                    "spa+eng", true, cache, 3, 0, null));
            PdfTextGeometryMap geometry = new PdfTextGeometryMap(pageNumber,
                    ocrPage.textLayer(), ocrPage.words(), "OCR_RASTER_PAGE",
                    ocrPage.warnings());
            List<PdfSemanticPageAnalysis.Element> elements = page.regions().stream()
                    .map(region -> element(page.widthPoints(), page.heightPoints(), region))
                    .toList();
            var analysis = new PdfSemanticPageAnalysis(pageNumber, "es",
                    PdfPageRole.CONTENT, elements, 1.0, List.of());
            var resolved = resolver.resolve(analysis,
                    nativeExtractor.extract(pdf, pageNumber), geometry, null,
                    page.widthPoints(), page.heightPoints(), cache, pageNumber).analysis();
            assertEquals(elements.size(), resolved.elements().size());
            BufferedImage overlay = pageNumber == 1
                    ? renderer.renderPage(new PdfPageRenderRequest(pdf, 1, 144,
                    PdfPageRenderRequest.DEFAULT_MAX_PIXEL_COUNT, Color.WHITE,
                    true)).image() : null;

            report.add("PAGE=" + pageNumber + "|ocrLines=" + ocrPage.lines().size()
                    + "|ocrWords=" + ocrPage.words().size() + "|ocrPasses=1");
            for (int index = 0; index < page.regions().size(); index++) {
                PdfRegion region = page.regions().get(index);
                var after = resolved.elements().get(index);
                boolean textual = TEXTUAL.contains(region.effectiveType());
                String geometryBoxes = after.attributes().getOrDefault(
                        "textGeometryBboxes", "");
                String wordBoxes = after.attributes().getOrDefault(
                        PdfTextVisualBoundsResolver.OCR_WORD_BBOXES_ATTRIBUTE, "");
                String tightBox = after.attributes().getOrDefault(
                        PdfTextVisualBoundsResolver.TIGHT_BBOX_ATTRIBUTE, "");
                int matchedWords = (int) parse(after.attributes().getOrDefault(
                        PdfTextVisualBoundsResolver.OCR_WORD_COUNT_ATTRIBUTE, "0"));
                double rightmostWordX = rightmostX(wordBoxes);
                double confidence = parse(after.attributes().getOrDefault(
                        "playbackGeometryConfidence", "0"));
                boolean confidentMatch = textual && confidence >= 0.55
                        && matchedWords > 0 && !tightBox.isBlank();
                String classification;
                if (confidentMatch) {
                    confident++;
                    classification = "EXACT_OCR";
                    sawTitle |= region.effectiveType() == PdfRegionType.TITLE;
                    sawCaption |= region.effectiveType() == PdfRegionType.CAPTION;
                    sawSidebar |= region.effectiveType() == PdfRegionType.SIDEBAR;
                    sawShortLine |= matchedWords <= 12;
                    sawMultiline |= geometryBoxes.contains(";");
                } else if (textual) {
                    classification = "SEMANTIC_FALLBACK";
                } else {
                    classification = "SEMANTIC_SPECIALIZED";
                }
                if (confidentMatch && !"OCR_RASTER_PAGE".equals(after.attributes()
                        .get("playbackGeometryOrigin"))) {
                    classification = "MISALIGNED";
                    misaligned++;
                }
                if (confidentMatch) {
                    double playbackRight = rightmostX(after.attributes()
                            .getOrDefault("playbackBboxes", ""));
                    assertEquals(rightmostWordX, playbackRight, 0.01,
                            "tight xMax must equal the rightmost matched OCR word");
                    if (overlay != null) drawVisualBounds(overlay, tightBox,
                            page.widthPoints(), page.heightPoints());
                }
                String visualSource = confidentMatch ? "OCR_WORDS"
                        : textual ? "SEMANTIC_FALLBACK" : "SPECIALIZED";
                String beforePadding = after.attributes().getOrDefault(
                        "playbackBboxes", semantic(region));
                String finalAtScale1 = confidentMatch
                        ? paddedAtScaleOne(beforePadding, page.widthPoints(),
                        page.heightPoints(), 12, 25)
                        : beforePadding;
                report.add(classification + "|type=" + region.effectiveType()
                        + "|regionId=" + region.id()
                        + "|semanticBBox=" + semantic(region)
                        + "|ocrText=" + oneLine(after.attributes().getOrDefault(
                        "textGeometryText", ""))
                        + "|ocrBBox=" + geometryBoxes
                        + "|matchedOcrWords=" + matchedWords
                        + "|ocrWordBboxes=" + wordBoxes
                        + "|rightmostWordX=" + rightmostWordX
                        + "|tightTextBBox=" + tightBox
                        + "|visualBoundsSource=" + visualSource
                        + "|visibleBBoxBeforePadding=" + beforePadding
                        + "|horizontalPaddingPxEachSide=" + (confidentMatch ? 12 : 0)
                        + "|verticalPaddingPxEachSide=" + (confidentMatch ? 25 : 0)
                        + "|finalBBoxAtViewportScale1=" + finalAtScale1
                        + "|confidence=" + confidence
                        + "|reason=" + after.attributes().getOrDefault(
                        "playbackGeometryReason", "semantic-specialized"));
            }
            if (overlay != null) {
                Path visual = Path.of("target/pdf-ocr-playback-grounding/p1-text-bounds.png");
                Files.createDirectories(visual.getParent());
                ImageIO.write(overlay, "png", visual.toFile());
            }
        }
        report.add("TOTAL|OCR_PASSES=3|VLM_CALLS=0|CONFIDENT=" + confident
                + "|MISALIGNED=" + misaligned);
        Path output = Path.of("target/pdf-ocr-playback-grounding/physical-report.txt");
        Files.createDirectories(output.getParent());
        Files.write(output, report, StandardCharsets.UTF_8);
        assertTrue(confident > 0, "No confident OCR/text region matches were found");
        assertEquals(0, misaligned);
        assertTrue(sawTitle, "P1/P2/P3 must exercise a grounded title");
        assertTrue(sawShortLine, "P1/P2/P3 must exercise a short textual line");
        assertTrue(sawMultiline, "P1/P2/P3 must exercise a multiline paragraph");
        assertTrue(sawCaption, "P1/P2/P3 must exercise a caption");
        assertTrue(sawSidebar, "P1/P2/P3 must exercise a sidebar");
    }

    private static Path canonicalWorkspace(int page) {
        String property = System.getProperty("docupodcast.pdf.grounding.workspace.p" + page);
        if (property != null && !property.isBlank()) return Path.of(property);
        return switch (page) {
            case 1 -> Path.of("target/pdf-final-acceptance/20260813-034719/workspaces/demo");
            case 2 -> Path.of("target/pdf-final-acceptance/20260811-033300/workspaces/demo");
            case 3 -> Path.of("target/pdf-final-acceptance/20260811-040907/workspaces/demo");
            default -> throw new IllegalArgumentException("page");
        };
    }

    private static PdfSemanticPageAnalysis.Element element(
            double width, double height, PdfRegion region) {
        Map<String, String> attributes = new LinkedHashMap<>(region.attributes());
        return new PdfSemanticPageAnalysis.Element(region.id(),
                region.effectiveReadingOrder(), region.effectiveType(),
                new PdfSemanticPageAnalysis.NormalizedBox(
                        region.xMin() / width * 1000.0,
                        region.yMin() / height * 1000.0,
                        region.xMax() / width * 1000.0,
                        region.yMax() / height * 1000.0),
                region.effectiveText(), "", region.effectiveNarratability(),
                region.evidence().confidence(), region.reasons(), attributes);
    }

    private static double parse(String value) {
        try { return Double.parseDouble(value); }
        catch (NumberFormatException ignored) { return 0.0; }
    }

    private static double rightmostX(String boxes) {
        if (boxes == null || boxes.isBlank()) return 0.0;
        double result = 0.0;
        for (String encoded : boxes.split(";")) {
            String[] coordinates = encoded.split(",", 4);
            if (coordinates.length == 4) {
                result = Math.max(result, parse(coordinates[2]));
            }
        }
        return result;
    }

    private static String paddedAtScaleOne(String encoded, double pageWidth,
                                           double pageHeight, double horizontal,
                                           double vertical) {
        if (encoded == null || encoded.isBlank()) return "";
        String[] coordinates = encoded.split("[;,]", 5);
        if (coordinates.length < 4) return encoded;
        double xMin = Math.max(0.0, parse(coordinates[0]) - horizontal);
        double yMin = Math.max(0.0, parse(coordinates[1]) - vertical);
        double xMax = Math.min(pageWidth, parse(coordinates[2]) + horizontal);
        double yMax = Math.min(pageHeight, parse(coordinates[3]) + vertical);
        return String.format(Locale.ROOT, "%.3f,%.3f,%.3f,%.3f",
                xMin, yMin, xMax, yMax);
    }

    private static void drawVisualBounds(BufferedImage image, String encoded,
                                         double pageWidth, double pageHeight) {
        String[] coordinates = encoded == null ? new String[0]
                : encoded.split(",", 4);
        if (coordinates.length != 4) return;
        double scaleX = image.getWidth() / pageWidth;
        double scaleY = image.getHeight() / pageHeight;
        int x = (int) Math.floor(parse(coordinates[0]) * scaleX);
        int y = (int) Math.floor(parse(coordinates[1]) * scaleY);
        int right = (int) Math.ceil(parse(coordinates[2]) * scaleX);
        int bottom = (int) Math.ceil(parse(coordinates[3]) * scaleY);
        int paddedX = Math.max(0, x - 12);
        int paddedY = Math.max(0, y - 25);
        int paddedRight = Math.min(image.getWidth(), right + 12);
        int paddedBottom = Math.min(image.getHeight(), bottom + 25);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(91, 76, 219, 56));
            graphics.fillRect(paddedX, paddedY, paddedRight - paddedX,
                    paddedBottom - paddedY);
            graphics.setColor(new Color(25, 145, 90, 220));
            graphics.setStroke(new BasicStroke(2.0f));
            graphics.drawRect(x, y, Math.max(1, right - x),
                    Math.max(1, bottom - y));
        } finally {
            graphics.dispose();
        }
    }

    private static String semantic(PdfRegion region) {
        return String.format(Locale.ROOT, "%.3f,%.3f,%.3f,%.3f",
                region.xMin(), region.yMin(), region.xMax(), region.yMax());
    }

    private static String oneLine(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").strip();
    }
}
