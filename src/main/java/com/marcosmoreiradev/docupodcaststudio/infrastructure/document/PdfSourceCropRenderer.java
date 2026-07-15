package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfCropRenderRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRenderRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRenderResult;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfRenderEngine;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfRenderException;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRequest;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessResult;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.application.document.SourceCropRenderer;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.process.DefaultExternalProcessRunner;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Renders source PDF page crops for study problems using Poppler and project-local PNG files. */
public final class PdfSourceCropRenderer implements SourceCropRenderer {
    private static final int RENDER_DPI = 144;
    private static final double PADDING_POINTS = 6.0;
    private static final long MAX_CROP_RENDER_PIXELS = PdfPageRenderRequest.DEFAULT_MAX_PIXEL_COUNT;
    private final PdfRenderEngine renderEngine;
    private final ExternalProcessRunner runner;

    public PdfSourceCropRenderer() {
        this(new PdfBoxRenderEngine(), new DefaultExternalProcessRunner());
    }

    PdfSourceCropRenderer(ExternalProcessRunner runner) {
        this(new PdfBoxRenderEngine(), runner);
    }

    PdfSourceCropRenderer(PdfRenderEngine renderEngine, ExternalProcessRunner runner) {
        this.renderEngine = renderEngine == null ? new PdfBoxRenderEngine() : renderEngine;
        this.runner = runner == null ? new DefaultExternalProcessRunner() : runner;
    }

    @Override
    public Optional<Path> renderBlockCrop(Path sourcePdf, DocumentBlock block, Path target) throws IOException {
        if (sourcePdf == null || block == null || target == null || !Files.isRegularFile(sourcePdf)) {
            return Optional.empty();
        }
        Optional<Integer> page = positiveInt(block.metadata().get("sourcePage"));
        Optional<PdfBox> bbox = PdfBox.parse(block.metadata().get("bbox"));
        if (page.isEmpty() || bbox.isEmpty()) {
            return Optional.empty();
        }
        Optional<Path> embedded = renderEmbeddedCrop(sourcePdf, page.get(), bbox.get(), target);
        if (embedded.isPresent()) {
            return embedded;
        }
        Optional<Double> pageWidth = positiveDouble(block.metadata().get("pageWidth"));
        Optional<Double> pageHeight = positiveDouble(block.metadata().get("pageHeight"));
        if (pageWidth.isEmpty() || pageHeight.isEmpty()) {
            return Optional.empty();
        }
        Path tempDir = Files.createTempDirectory("docupodcast-pdf-crop");
        try {
            Path renderedPage = renderPage(sourcePdf, page.get(), tempDir);
            if (!Files.isRegularFile(renderedPage)) {
                return Optional.empty();
            }
            BufferedImage pageImage = ImageIO.read(renderedPage.toFile());
            if (pageImage == null) {
                return Optional.empty();
            }
            Optional<CropArea> area = cropArea(bbox.get(), pageWidth.get(), pageHeight.get(),
                    pageImage.getWidth(), pageImage.getHeight(), PADDING_POINTS);
            if (area.isEmpty()) {
                return Optional.empty();
            }
            writeCrop(pageImage, area.get(), target);
            return Optional.of(target);
        } finally {
            deleteQuietly(tempDir);
        }
    }

    private Optional<Path> renderEmbeddedCrop(Path sourcePdf, int page, PdfBox bbox, Path target) throws IOException {
        try {
            PdfPageRenderResult result = renderEngine.renderCrop(new PdfCropRenderRequest(
                    sourcePdf,
                    page,
                    bbox.xMin(),
                    bbox.yMin(),
                    bbox.xMax(),
                    bbox.yMax(),
                    PADDING_POINTS,
                    RENDER_DPI,
                    MAX_CROP_RENDER_PIXELS,
                    Color.WHITE,
                    true));
            writeImage(result.image(), target);
            return Optional.of(target);
        } catch (PdfRenderException ex) {
            return Optional.empty();
        }
    }

    private Path renderPage(Path sourcePdf, int page, Path tempDir) throws IOException {
        Path prefix = tempDir.resolve("page");
        List<String> args = List.of(
                "-f", Integer.toString(page),
                "-l", Integer.toString(page),
                "-singlefile",
                "-r", Integer.toString(RENDER_DPI),
                "-png",
                sourcePdf.toString(),
                prefix.toString());
        ExternalProcessResult result = runPdftoppm(args);
        return result.succeeded() ? Path.of(prefix + ".png") : tempDir.resolve("missing.png");
    }

    private ExternalProcessResult runPdftoppm(List<String> args) throws IOException {
        IOException firstFailure = null;
        for (String executable : pdftoppmExecutables()) {
            try {
                java.util.ArrayList<String> command = new java.util.ArrayList<>();
                command.add(executable);
                command.addAll(args);
                ExternalProcessResult result = runner.run(
                        ExternalProcessRequest.of(command, "pdf-source-crop", Duration.ofSeconds(20)));
                if (result.succeeded()) {
                    return result;
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return new ExternalProcessResult(-2, false, true, "", ex.getMessage(), "pdf-source-crop", Duration.ZERO);
            } catch (IOException ex) {
                if (firstFailure == null) {
                    firstFailure = ex;
                }
            }
        }
        if (firstFailure != null) {
            throw firstFailure;
        }
        return new ExternalProcessResult(1, false, false, "", "pdftoppm no disponible", "pdf-source-crop", Duration.ZERO);
    }

    private static List<String> pdftoppmExecutables() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win")
                ? List.of("pdftoppm", "pdftoppm.exe", "pdftoppm.cmd")
                : List.of("pdftoppm");
    }

    static Optional<CropArea> cropArea(PdfBox bbox, double pageWidth, double pageHeight,
                                       int imageWidth, int imageHeight, double paddingPoints) {
        if (bbox == null || !bbox.valid() || pageWidth <= 0 || pageHeight <= 0 || imageWidth <= 0 || imageHeight <= 0) {
            return Optional.empty();
        }
        double scaleX = imageWidth / pageWidth;
        double scaleY = imageHeight / pageHeight;
        double padding = Math.max(0.0, paddingPoints);
        int x = clamp((int) Math.floor((bbox.xMin() - padding) * scaleX), 0, imageWidth - 1);
        int y = clamp((int) Math.floor((bbox.yMin() - padding) * scaleY), 0, imageHeight - 1);
        int right = clamp((int) Math.ceil((bbox.xMax() + padding) * scaleX), x + 1, imageWidth);
        int bottom = clamp((int) Math.ceil((bbox.yMax() + padding) * scaleY), y + 1, imageHeight);
        CropArea area = new CropArea(x, y, right - x, bottom - y);
        return area.valid() ? Optional.of(area) : Optional.empty();
    }

    static void writeCrop(BufferedImage source, CropArea area, Path target) throws IOException {
        if (source == null || area == null || !area.valid()) {
            throw new IOException("Crop PDF invalido.");
        }
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        BufferedImage crop = new BufferedImage(area.width(), area.height(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = crop.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, area.width(), area.height(),
                    area.x(), area.y(), area.x() + area.width(), area.y() + area.height(), null);
        } finally {
            graphics.dispose();
        }
        if (!ImageIO.write(crop, "png", target.toFile())) {
            throw new IOException("No se pudo escribir crop PNG de PDF.");
        }
    }

    private static void writeImage(BufferedImage image, Path target) throws IOException {
        if (image == null) {
            throw new IOException("Crop PDF invalido.");
        }
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        if (!ImageIO.write(image, "png", target.toFile())) {
            throw new IOException("No se pudo escribir crop PNG de PDF.");
        }
    }

    private static Optional<Integer> positiveInt(String value) {
        try {
            int parsed = Integer.parseInt(value == null ? "" : value.strip());
            return parsed > 0 ? Optional.of(parsed) : Optional.empty();
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private static Optional<Double> positiveDouble(String value) {
        try {
            double parsed = Double.parseDouble(value == null ? "" : value.strip());
            return parsed > 0 && Double.isFinite(parsed) ? Optional.of(parsed) : Optional.empty();
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void deleteQuietly(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    record CropArea(int x, int y, int width, int height) {
        boolean valid() {
            return x >= 0 && y >= 0 && width > 0 && height > 0;
        }
    }
}
