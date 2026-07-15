package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemPdfExporter;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemPdfPage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** PDFBox implementation for batch-exporting technical-problem solution images. */
public final class PdfBoxStudyProblemPdfExporter implements StudyProblemPdfExporter {
    @Override
    public void export(List<StudyProblemPdfPage> pages, Path targetPdf) throws IOException {
        if (pages == null || pages.isEmpty()) {
            throw new IOException("No hay ejercicios con PNG final para exportar a PDF.");
        }
        Path output = requirePdfTarget(targetPdf);
        Files.createDirectories(output.getParent());
        try (PDDocument pdf = new PDDocument()) {
            for (StudyProblemPdfPage page : pages) {
                BufferedImage image = ImageIO.read(page.imagePath().toFile());
                if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                    throw new IOException("PNG de solucion no legible: " + page.imagePath().getFileName());
                }
                addImagePage(pdf, image);
            }
            pdf.save(output.toFile());
        }
    }

    private static void addImagePage(PDDocument pdf, BufferedImage image) throws IOException {
        PDRectangle base = PDRectangle.A4;
        boolean landscape = image.getWidth() > image.getHeight();
        PDRectangle pageSize = landscape ? new PDRectangle(base.getHeight(), base.getWidth()) : base;
        PDPage page = new PDPage(pageSize);
        pdf.addPage(page);
        PDImageXObject xObject = LosslessFactory.createFromImage(pdf, image);
        try (PDPageContentStream stream = new PDPageContentStream(pdf, page)) {
            stream.setNonStrokingColor(Color.WHITE);
            stream.addRect(0, 0, pageSize.getWidth(), pageSize.getHeight());
            stream.fill();
            float margin = 28f;
            float availableWidth = pageSize.getWidth() - margin * 2f;
            float availableHeight = pageSize.getHeight() - margin * 2f;
            float scale = Math.min(availableWidth / image.getWidth(), availableHeight / image.getHeight());
            float drawWidth = image.getWidth() * scale;
            float drawHeight = image.getHeight() * scale;
            float x = (pageSize.getWidth() - drawWidth) / 2f;
            float y = (pageSize.getHeight() - drawHeight) / 2f;
            stream.drawImage(xObject, x, y, drawWidth, drawHeight);
        }
    }

    private static Path requirePdfTarget(Path targetPdf) throws IOException {
        if (targetPdf == null) {
            throw new IOException("Selecciona un archivo PDF destino.");
        }
        Path output = targetPdf.toAbsolutePath().normalize();
        if (output.getParent() == null) {
            throw new IOException("El PDF destino debe tener carpeta padre.");
        }
        return output;
    }
}
