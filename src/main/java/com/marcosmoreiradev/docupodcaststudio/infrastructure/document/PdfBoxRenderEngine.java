package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfCropRenderRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfDocumentInfo;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOpenOptions;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageInfo;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRenderRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRenderResult;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfRenderEngine;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfRenderErrorCode;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfRenderException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.interactive.annotation.AnnotationFilter;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Embedded PDF renderer backed by Apache PDFBox. */
public final class PdfBoxRenderEngine implements PdfRenderEngine {
    private static final int MIN_DPI = 24;
    private static final int MAX_DPI = 300;

    @Override
    public PdfDocumentInfo inspect(Path sourcePdf, PdfOpenOptions options) throws PdfRenderException {
        Path source = requirePdfFile(sourcePdf);
        ensureEmptyPassword(options);
        try (PDDocument document = Loader.loadPDF(source.toFile())) {
            List<PdfPageInfo> pages = new ArrayList<>();
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                PDPage page = document.getPage(i);
                PDRectangle box = pageBox(page);
                pages.add(new PdfPageInfo(i + 1, box.getWidth(), box.getHeight(), page.getRotation()));
            }
            PDDocumentInformation information = document.getDocumentInformation();
            String title = information == null ? "" : information.getTitle();
            return new PdfDocumentInfo(source, document.getNumberOfPages(), document.isEncrypted(),
                    true, title, pages, List.of("pdfbox-render-engine"));
        } catch (InvalidPasswordException ex) {
            throw new PdfRenderException(PdfRenderErrorCode.PASSWORD_REQUIRED,
                    "El PDF requiere contrasena para renderizarse.", ex);
        } catch (IOException | RuntimeException ex) {
            throw new PdfRenderException(PdfRenderErrorCode.INVALID_PDF,
                    "No se pudo inspeccionar el PDF.", ex);
        }
    }

    @Override
    public PdfPageRenderResult renderPage(PdfPageRenderRequest request) throws PdfRenderException {
        Objects.requireNonNull(request, "request");
        Path source = requirePdfFile(request.sourcePath());
        try (PDDocument document = Loader.loadPDF(source.toFile())) {
            int pageIndex = validatePage(document, request.pageNumber());
            PDPage page = document.getPage(pageIndex);
            PDRectangle box = pageBox(page);
            int safeDpi = safeDpi(box.getWidth(), box.getHeight(), request.dpi(), request.maxPixelCount());
            PDFRenderer renderer = new PDFRenderer(document);
            renderer.setSubsamplingAllowed(false);
            if (!request.renderAnnotations()) {
                renderer.setAnnotationsFilter(noAnnotations());
            }
            BufferedImage rendered = renderer.renderImageWithDPI(pageIndex, safeDpi, ImageType.RGB);
            BufferedImage image = opaqueCopy(rendered, request.backgroundColor());
            int rotation = Math.floorMod(page.getRotation(), 360);
            boolean swapsAxes = rotation == 90 || rotation == 270;
            return new PdfPageRenderResult(request.pageNumber(), document.getNumberOfPages(),
                    swapsAxes ? box.getHeight() : box.getWidth(),
                    swapsAxes ? box.getWidth() : box.getHeight(),
                    safeDpi, image, "pdfbox-page",
                    renderWarnings(request.dpi(), safeDpi));
        } catch (InvalidPasswordException ex) {
            throw new PdfRenderException(PdfRenderErrorCode.PASSWORD_REQUIRED,
                    "El PDF requiere contrasena para renderizarse.", ex);
        } catch (PdfRenderException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new PdfRenderException(PdfRenderErrorCode.RENDER_FAILED,
                    "No se pudo renderizar la pagina PDF.", ex);
        }
    }

    @Override
    public PdfPageRenderResult renderCrop(PdfCropRenderRequest request) throws PdfRenderException {
        Objects.requireNonNull(request, "request");
        if (!(request.xMaxPoints() > request.xMinPoints()) || !(request.yMaxPoints() > request.yMinPoints())) {
            throw new PdfRenderException(PdfRenderErrorCode.RENDER_FAILED, "BBox PDF invalido para crop.");
        }
        PdfPageRenderResult page = renderPage(new PdfPageRenderRequest(
                request.sourcePath(),
                request.pageNumber(),
                request.dpi(),
                request.maxPixelCount(),
                request.backgroundColor(),
                request.renderAnnotations()));
        BufferedImage crop = cropImage(page.image(), page.pageWidthPoints(), page.pageHeightPoints(),
                request.xMinPoints(), request.yMinPoints(), request.xMaxPoints(), request.yMaxPoints(), request.paddingPoints());
        return new PdfPageRenderResult(page.pageNumber(), page.pageCount(), page.pageWidthPoints(),
                page.pageHeightPoints(), page.dpi(), crop, "pdfbox-crop", page.warnings());
    }

    private static Path requirePdfFile(Path sourcePdf) throws PdfRenderException {
        if (sourcePdf == null || !Files.isRegularFile(sourcePdf)) {
            throw new PdfRenderException(PdfRenderErrorCode.INVALID_PDF, "El PDF no existe o no es un archivo regular.");
        }
        return sourcePdf.toAbsolutePath().normalize();
    }

    private static void ensureEmptyPassword(PdfOpenOptions options) throws PdfRenderException {
        PdfOpenOptions safe = options == null ? PdfOpenOptions.empty() : options;
        if (safe.hasPassword()) {
            throw new PdfRenderException(PdfRenderErrorCode.UNSUPPORTED_FEATURE,
                    "La apertura con contrasena queda pendiente hasta tener UI para solicitarla.");
        }
    }

    private static int validatePage(PDDocument document, int pageNumber) throws PdfRenderException {
        if (pageNumber <= 0 || pageNumber > document.getNumberOfPages()) {
            throw new PdfRenderException(PdfRenderErrorCode.PAGE_OUT_OF_RANGE,
                    "Pagina PDF fuera de rango: " + pageNumber + ".");
        }
        return pageNumber - 1;
    }

    private static PDRectangle pageBox(PDPage page) {
        PDRectangle crop = page == null ? null : page.getCropBox();
        if (crop != null && crop.getWidth() > 0 && crop.getHeight() > 0) {
            return crop;
        }
        return page == null ? PDRectangle.LETTER : page.getMediaBox();
    }

    private static int safeDpi(double widthPoints, double heightPoints, int requestedDpi, long maxPixelCount) throws PdfRenderException {
        int dpi = Math.max(MIN_DPI, Math.min(MAX_DPI, requestedDpi <= 0 ? PdfPageRenderRequest.DEFAULT_DPI : requestedDpi));
        long limit = maxPixelCount <= 0 ? PdfPageRenderRequest.DEFAULT_MAX_PIXEL_COUNT : maxPixelCount;
        while (dpi > MIN_DPI && estimatedPixels(widthPoints, heightPoints, dpi) > limit) {
            dpi--;
        }
        if (estimatedPixels(widthPoints, heightPoints, dpi) > limit) {
            throw new PdfRenderException(PdfRenderErrorCode.TOO_LARGE,
                    "La pagina PDF excede el limite de pixeles incluso al DPI minimo seguro.");
        }
        return dpi;
    }

    private static long estimatedPixels(double widthPoints, double heightPoints, int dpi) {
        long width = Math.max(1L, (long) Math.ceil((Math.max(1.0, widthPoints) / 72.0) * dpi));
        long height = Math.max(1L, (long) Math.ceil((Math.max(1.0, heightPoints) / 72.0) * dpi));
        return width * height;
    }

    private static BufferedImage opaqueCopy(BufferedImage rendered, Color background) {
        BufferedImage output = new BufferedImage(rendered.getWidth(), rendered.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setColor(background == null ? Color.WHITE : background);
            graphics.fillRect(0, 0, output.getWidth(), output.getHeight());
            graphics.drawImage(rendered, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return output;
    }

    private static AnnotationFilter noAnnotations() {
        return annotation -> false;
    }

    private static BufferedImage cropImage(BufferedImage source, double pageWidthPoints, double pageHeightPoints,
                                           double xMin, double yMin, double xMax, double yMax, double paddingPoints) throws PdfRenderException {
        double scaleX = source.getWidth() / Math.max(1.0, pageWidthPoints);
        double scaleY = source.getHeight() / Math.max(1.0, pageHeightPoints);
        double padding = Math.max(0.0, paddingPoints);
        int x = clamp((int) Math.floor((xMin - padding) * scaleX), 0, source.getWidth() - 1);
        int y = clamp((int) Math.floor((yMin - padding) * scaleY), 0, source.getHeight() - 1);
        int right = clamp((int) Math.ceil((xMax + padding) * scaleX), x + 1, source.getWidth());
        int bottom = clamp((int) Math.ceil((yMax + padding) * scaleY), y + 1, source.getHeight());
        if (right <= x || bottom <= y) {
            throw new PdfRenderException(PdfRenderErrorCode.RENDER_FAILED, "Crop PDF vacio despues de aplicar limites.");
        }
        BufferedImage output = new BufferedImage(right - x, bottom - y, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, output.getWidth(), output.getHeight(), x, y, right, bottom, null);
        } finally {
            graphics.dispose();
        }
        return output;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static List<String> renderWarnings(int requestedDpi, int safeDpi) {
        if (requestedDpi > safeDpi) {
            return List.of("dpi-reduced:" + safeDpi);
        }
        return List.of();
    }
}
