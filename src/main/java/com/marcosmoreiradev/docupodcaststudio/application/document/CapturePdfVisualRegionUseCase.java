package com.marcosmoreiradev.docupodcaststudio.application.document;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Renders a user-selected PDF viewport rectangle into a temporary or caller-provided PNG. */
public final class CapturePdfVisualRegionUseCase {
    private final PdfRenderEngine renderEngine;

    public CapturePdfVisualRegionUseCase(PdfRenderEngine renderEngine) {
        this.renderEngine = Objects.requireNonNull(renderEngine, "renderEngine");
    }

    public PdfRegionCaptureResult capture(PdfRegionCaptureRequest request) throws PdfRenderException, IOException {
        Objects.requireNonNull(request, "request");
        PdfPageRegion region = request.selection().toPageRegion();
        PdfPageRenderResult rendered = renderEngine.renderCrop(new PdfCropRenderRequest(
                request.sourcePath(),
                region.pageNumber(),
                region.xMinPoints(),
                region.yMinPoints(),
                region.xMaxPoints(),
                region.yMaxPoints(),
                request.paddingPoints(),
                request.dpi(),
                request.maxPixelCount(),
                request.backgroundColor(),
                request.renderAnnotations()));
        Path target = request.targetPng() == null ? temporaryTarget() : request.targetPng().toAbsolutePath().normalize();
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        if (!ImageIO.write(rendered.image(), "png", target.toFile())) {
            throw new IOException("No se pudo escribir captura PNG de region PDF.");
        }
        return new PdfRegionCaptureResult(
                region.pageNumber(),
                region.bbox(),
                rendered.dpi(),
                rendered.image().getWidth(),
                rendered.image().getHeight(),
                target,
                rendered.warnings());
    }

    private static Path temporaryTarget() throws IOException {
        Path directory = Files.createTempDirectory("docupodcast-pdf-region");
        return directory.resolve("region.png");
    }
}
