package com.marcosmoreiradev.docupodcaststudio.application.document;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Builds the ROI and a lower-resolution page with the ROI marked. Inference
 * remains outside this PDF adapter.
 */
public final class CapturePdfAnalysisVisualEvidenceUseCase {
    private final PdfRenderEngine renderEngine;

    public CapturePdfAnalysisVisualEvidenceUseCase(PdfRenderEngine renderEngine) {
        this.renderEngine = Objects.requireNonNull(renderEngine, "renderEngine");
    }

    public PdfAnalysisVisualEvidence capture(Path sourcePdf, PdfPageRegion region)
            throws IOException, PdfRenderException {
        Objects.requireNonNull(sourcePdf, "sourcePdf");
        Objects.requireNonNull(region, "region");
        Path directory = Files.createTempDirectory("docupodcast-analysis-visual-");
        Path roi = directory.resolve("roi.png");
        Path context = directory.resolve("page-with-roi.png");
        try {
            PdfPageRenderResult crop = renderEngine.renderCrop(new PdfCropRenderRequest(
                    sourcePdf, region.pageNumber(), region.xMinPoints(), region.yMinPoints(),
                    region.xMaxPoints(), region.yMaxPoints(), 12.0, 288,
                    32_000_000L, Color.WHITE, true));
            write(crop.image(), roi);
            PdfPageRenderResult page = renderEngine.renderPage(new PdfPageRenderRequest(
                    sourcePdf, region.pageNumber(), 120,
                    24_000_000L, Color.WHITE, true));
            BufferedImage marked = copy(page.image());
            mark(marked, page, region);
            write(marked, context);
            return new PdfAnalysisVisualEvidence(roi, context);
        } catch (IOException failure) {
            Files.deleteIfExists(roi);
            Files.deleteIfExists(context);
            Files.deleteIfExists(directory);
            throw failure;
        }
    }

    private static void mark(BufferedImage image, PdfPageRenderResult page, PdfPageRegion region) {
        double scaleX = image.getWidth() / page.pageWidthPoints();
        double scaleY = image.getHeight() / page.pageHeightPoints();
        int x = (int) Math.round(region.xMinPoints() * scaleX);
        int y = (int) Math.round(region.yMinPoints() * scaleY);
        int width = Math.max(2, (int) Math.round(
                (region.xMaxPoints() - region.xMinPoints()) * scaleX));
        int height = Math.max(2, (int) Math.round(
                (region.yMaxPoints() - region.yMinPoints()) * scaleY));
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(255, 200, 0, 48));
            graphics.fillRect(x, y, width, height);
            graphics.setColor(new Color(165, 70, 0));
            graphics.setStroke(new BasicStroke(5.0f));
            graphics.drawRect(x, y, width, height);
        } finally {
            graphics.dispose();
        }
    }

    private static BufferedImage copy(BufferedImage source) {
        BufferedImage result = new BufferedImage(
                source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private static void write(BufferedImage image, Path target) throws IOException {
        if (!ImageIO.write(image, "png", target.toFile())) {
            throw new IOException("No se pudo escribir la evidencia visual PNG.");
        }
    }
}
