package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfNativePageExtractor;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRegion;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextLayer;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextLayerOrigin;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextLine;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextToken;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Shared native text and geometry for direct reading and semantic coverage evidence. */
public final class PdfBoxNativeTextEvidenceExtractor
        implements PdfNativePageExtractor {
    @Override
    public PdfTextLayer extract(Path sourcePdf, int pageNumber) {
        if (sourcePdf == null || !Files.isRegularFile(sourcePdf)
                || pageNumber <= 0) {
            return unavailable(pageNumber, "Solicitud de evidencia PDFBox invalida.");
        }
        try (PDDocument document = Loader.loadPDF(sourcePdf.toFile())) {
            if (!document.getCurrentAccessPermission().canExtractContent()) {
                return unavailable(pageNumber, "El PDF no permite extraer su contenido.");
            }
            if (pageNumber > document.getNumberOfPages()) {
                return unavailable(pageNumber, "Pagina fuera de rango para PDFBox.");
            }
            PDPage page = document.getPage(pageNumber - 1);
            PDRectangle crop = page.getCropBox() == null
                    ? page.getMediaBox() : page.getCropBox();
            double width = Math.max(1.0, crop.getWidth());
            double height = Math.max(1.0, crop.getHeight());
            CapturingStripper stripper = new CapturingStripper(
                    pageNumber, width, height, page.getRotation());
            stripper.setStartPage(pageNumber);
            stripper.setEndPage(pageNumber);
            stripper.setSortByPosition(true);
            stripper.getText(document);
            if (stripper.lines.isEmpty()) {
                return unavailable(pageNumber,
                        "PDFBox no encontro texto nativo util.");
            }
            return new PdfTextLayer(pageNumber, PdfTextLayerOrigin.NATIVE_BBOX,
                    coalesceFragments(stripper.lines),
                    List.of());
        } catch (IOException | RuntimeException failure) {
            return unavailable(pageNumber,
                    "Evidencia PDFBox no disponible: " + failure.getMessage());
        }
    }

    private static PdfTextLayer unavailable(int page, String warning) {
        return new PdfTextLayer(Math.max(1, page),
                PdfTextLayerOrigin.UNAVAILABLE, List.of(), List.of(warning));
    }

    private static List<PdfTextLine> coalesceFragments(
            List<PdfTextLine> fragments) {
        ArrayList<PdfTextLine> ordered = new ArrayList<>(fragments);
        ordered.sort(Comparator
                .comparingDouble((PdfTextLine line) -> line.region().yMinPoints())
                .thenComparingDouble(line -> line.region().xMinPoints()));
        ArrayList<PdfTextLine> result = new ArrayList<>();
        for (PdfTextLine fragment : ordered) {
            int mergeAt = -1;
            for (int index = result.size() - 1; index >= 0; index--) {
                PdfTextLine candidate = result.get(index);
                if (candidate.region().yMinPoints()
                        < fragment.region().yMinPoints() - 18.0) break;
                if (sameVisualLine(candidate.region(), fragment.region())) {
                    mergeAt = index;
                    break;
                }
            }
            if (mergeAt < 0) {
                result.add(fragment);
            } else {
                result.set(mergeAt, merge(result.get(mergeAt), fragment));
            }
        }
        result.sort(Comparator
                .comparingDouble((PdfTextLine line) -> line.region().yMinPoints())
                .thenComparingDouble(line -> line.region().xMinPoints()));
        return List.copyOf(result);
    }

    private static boolean sameVisualLine(PdfPageRegion left,
                                          PdfPageRegion right) {
        double leftHeight = left.yMaxPoints() - left.yMinPoints();
        double rightHeight = right.yMaxPoints() - right.yMinPoints();
        double leftCenter = (left.yMinPoints() + left.yMaxPoints()) / 2.0;
        double rightCenter = (right.yMinPoints() + right.yMaxPoints()) / 2.0;
        double centerTolerance = Math.max(1.5,
                Math.min(leftHeight, rightHeight) * 0.45);
        if (Math.abs(leftCenter - rightCenter) > centerTolerance) return false;
        double gap = right.xMinPoints() - left.xMaxPoints();
        double gapTolerance = Math.max(18.0,
                Math.max(leftHeight, rightHeight) * 3.0);
        return gap >= -2.0 && gap <= gapTolerance;
    }

    private static PdfTextLine merge(PdfTextLine left, PdfTextLine right) {
        PdfPageRegion a = left.region();
        PdfPageRegion b = right.region();
        PdfPageRegion region = new PdfPageRegion(left.pageNumber(),
                Math.min(a.xMinPoints(), b.xMinPoints()),
                Math.min(a.yMinPoints(), b.yMinPoints()),
                Math.max(a.xMaxPoints(), b.xMaxPoints()),
                Math.max(a.yMaxPoints(), b.yMaxPoints()),
                a.pageWidthPoints(), a.pageHeightPoints());
        ArrayList<PdfTextToken> tokens = new ArrayList<>(left.tokens());
        tokens.addAll(right.tokens());
        String text = left.text() + (left.text().endsWith("-") ? "" : " ")
                + right.text();
        return new PdfTextLine(left.pageNumber(), text, region, tokens,
                Math.min(left.confidence(), right.confidence()));
    }

    private static final class CapturingStripper extends PDFTextStripper {
        private final int pageNumber;
        private final double pageWidth;
        private final double pageHeight;
        private final int rotation;
        private final ArrayList<PdfTextLine> lines = new ArrayList<>();

        private CapturingStripper(int pageNumber, double pageWidth,
                                  double pageHeight, int rotation) throws IOException {
            this.pageNumber = pageNumber;
            this.pageWidth = pageWidth;
            this.pageHeight = pageHeight;
            this.rotation = Math.floorMod(rotation, 360);
        }

        @Override
        protected void writeString(String text,
                                   List<TextPosition> positions) {
            String value = text == null ? "" : text.strip();
            if (value.isBlank() || positions == null || positions.isEmpty()) return;
            double xMin = positions.stream().mapToDouble(TextPosition::getXDirAdj)
                    .min().orElse(0.0);
            double xMax = positions.stream().mapToDouble(position ->
                    position.getXDirAdj() + position.getWidthDirAdj())
                    .max().orElse(xMin + 1.0);
            double baselineMin = positions.stream()
                    .mapToDouble(TextPosition::getYDirAdj).min().orElse(0.0);
            double baselineMax = positions.stream()
                    .mapToDouble(TextPosition::getYDirAdj).max().orElse(baselineMin);
            double glyphHeight = positions.stream()
                    .mapToDouble(TextPosition::getHeightDir).max().orElse(1.0);
            double yMin = Math.max(0.0, baselineMin - glyphHeight);
            double yMax = Math.min(pageHeight,
                    Math.max(yMin + 0.5, baselineMax + 0.5));
            xMin = Math.max(0.0, Math.min(pageWidth - 0.5, xMin));
            xMax = Math.max(xMin + 0.5, Math.min(pageWidth, xMax));
            PdfPageRegion region = rotatedRegion(xMin, yMin, xMax, yMax);
            PdfTextToken token = new PdfTextToken(value, region, 1.0);
            lines.add(new PdfTextLine(pageNumber, value, region,
                    List.of(token), 1.0));
        }

        private PdfPageRegion rotatedRegion(double left, double top, double right, double bottom) {
            return switch (rotation) {
                case 90 -> new PdfPageRegion(pageNumber, pageHeight - bottom, left,
                        pageHeight - top, right, pageHeight, pageWidth);
                case 180 -> new PdfPageRegion(pageNumber, pageWidth - right, pageHeight - bottom,
                        pageWidth - left, pageHeight - top, pageWidth, pageHeight);
                case 270 -> new PdfPageRegion(pageNumber, top, pageWidth - right,
                        bottom, pageWidth - left, pageHeight, pageWidth);
                default -> new PdfPageRegion(pageNumber, left, top, right, bottom, pageWidth, pageHeight);
            };
        }
    }
}
