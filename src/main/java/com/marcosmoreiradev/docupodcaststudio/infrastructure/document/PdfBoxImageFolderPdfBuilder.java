package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.ImageFolderPdfBuilder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** PDFBox-backed image-folder PDF builder. */
public final class PdfBoxImageFolderPdfBuilder implements ImageFolderPdfBuilder {
    private static final float MAX_PAGE_SIDE_POINTS = 900.0f;
    private static final float MIN_PAGE_SIDE_POINTS = 360.0f;

    @Override
    public Path build(List<Path> imageFiles, Path targetPdf) throws IOException {
        if (imageFiles == null || imageFiles.isEmpty()) {
            throw new IOException("No hay imagenes para crear el PDF.");
        }
        try (PDDocument document = new PDDocument()) {
            for (Path imageFile : imageFiles) {
                addImagePage(document, imageFile);
            }
            document.save(targetPdf.toFile());
        }
        return targetPdf;
    }

    private static void addImagePage(PDDocument document, Path imageFile) throws IOException {
        BufferedImage probe = ImageIO.read(imageFile.toFile());
        if (probe == null || probe.getWidth() <= 0 || probe.getHeight() <= 0) {
            throw new IOException("Imagen no compatible: " + imageFile.getFileName());
        }
        PDRectangle pageSize = pageSizeFor(probe.getWidth(), probe.getHeight());
        PDPage page = new PDPage(pageSize);
        document.addPage(page);
        PDImageXObject image = PDImageXObject.createFromFileByContent(imageFile.toFile(), document);
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            content.setNonStrokingColor(255, 255, 255);
            content.addRect(0, 0, pageSize.getWidth(), pageSize.getHeight());
            content.fill();
            content.drawImage(image, 0, 0, pageSize.getWidth(), pageSize.getHeight());
        }
    }

    private static PDRectangle pageSizeFor(int width, int height) {
        double ratio = width / (double) height;
        float pageWidth;
        float pageHeight;
        if (ratio >= 1.0) {
            pageWidth = MAX_PAGE_SIDE_POINTS;
            pageHeight = (float) (pageWidth / ratio);
        } else {
            pageHeight = MAX_PAGE_SIDE_POINTS;
            pageWidth = (float) (pageHeight * ratio);
        }
        pageWidth = Math.max(MIN_PAGE_SIDE_POINTS, pageWidth);
        pageHeight = Math.max(MIN_PAGE_SIDE_POINTS, pageHeight);
        return new PDRectangle(pageWidth, pageHeight);
    }
}
