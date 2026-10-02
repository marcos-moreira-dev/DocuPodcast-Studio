package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.*;
import org.apache.pdfbox.Loader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit local comparison; never requires Poppler in the default test suite. */
@EnabledIfSystemProperty(named = "pdf.compare.poppler", matches = "true")
final class PdfNativeAdapterComparisonTest {
    @TempDir Path temporary;
    @Test void comparesExistingNativePdfWithoutModels() throws Exception {
        // Compare a valid controlled fixture; leave the malformed historical sample untouched.
        Path source = temporary.resolve("native comparison.pdf");
        try (var pdf = new org.apache.pdfbox.pdmodel.PDDocument()) {
            var page = new org.apache.pdfbox.pdmodel.PDPage();
            pdf.addPage(page);
            try (var content = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdf, page)) {
                content.beginText();
                content.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(
                        org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(60, 700);
                content.showText("Lectura directa de un documento universitario.");
                content.newLineAtOffset(0, -24);
                content.showText("El texto conserva su pagina y sus coordenadas.");
                content.endText();
            }
            pdf.save(source.toFile());
        }
        int pages;
        try (var pdf = Loader.loadPDF(source.toFile())) { pages = pdf.getNumberOfPages(); }
        var report = new ArrayList<String>();
        report.add("page,provider,milliseconds,characters,lines,heapBytes");
        var adapters = Map.<String, PdfNativePageExtractor>of("PDFBox", new PdfBoxNativeTextEvidenceExtractor(),
                "Poppler", new PopplerPdfNativePageExtractor());
        for (int page = 1; page <= Math.min(5, pages); page++) {
            var texts = new HashMap<String, String>();
            for (var entry : adapters.entrySet()) {
                long start = System.nanoTime();
                PdfTextLayer layer = entry.getValue().extract(source, page);
                long millis = (System.nanoTime() - start) / 1_000_000;
                assertTrue(layer.available(), entry.getKey() + ": " + layer.warnings());
                String text = layer.lines().stream().map(PdfTextLine::text).collect(java.util.stream.Collectors.joining(" "));
                texts.put(entry.getKey(), text);
                assertTrue(layer.lines().stream().allMatch(line -> line.region().xMinPoints() >= 0
                        && line.region().yMinPoints() >= 0
                        && line.region().xMaxPoints() <= line.region().pageWidthPoints()
                        && line.region().yMaxPoints() <= line.region().pageHeightPoints()));
                report.add(page + "," + entry.getKey() + "," + millis + "," + text.length() + ","
                        + layer.lines().size() + "," + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
            }
            Set<String> first = new HashSet<>(List.of(texts.get("PDFBox").split("\\s+")));
            Set<String> second = new HashSet<>(List.of(texts.get("Poppler").split("\\s+")));
            Set<String> common = new HashSet<>(first);
            common.retainAll(second);
            assertTrue(common.size() >= Math.min(first.size(), second.size()) * 0.9,
                    "Native adapters disagree on more than 10% of distinct words");
        }
        Path reportPath = Path.of("target/pdf-native-comparison.csv");
        Files.createDirectories(reportPath.toAbsolutePath().getParent());
        Files.write(reportPath, report);
    }
}
