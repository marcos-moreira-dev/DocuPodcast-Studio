package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfNativeTextQualityAssessor;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfNativeTextQualityReport;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRenderRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRenderResult;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextLayer;
import org.apache.pdfbox.Loader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opt-in evidence harness for the registered real books. It never copies or mutates
 * the originals and writes only non-canonical evidence under target/.
 */
@EnabledIfSystemProperty(named = "docupodcast.pdf.realCorpus", matches = "true")
class PdfRealBooksCorpusTest {
    private static final Path OUTPUT =
            Path.of("target/pdf-real-corpus").toAbsolutePath().normalize();
    private static final List<Book> BOOKS = List.of(
            new Book("kress",
                    Path.of("C:/Users/MARCOS MOREIRA/Desktop/Numerical Analysis - Rainer Kress.pdf"),
                    342, "2631606a9815ebd76573160f825d6be0e95fd35c3e731fa0abd52671cbf20cdf",
                    List.of(20, 100, 340)),
            new Book("burden",
                    Path.of("C:/Users/MARCOS MOREIRA/Desktop/"
                            + "Numerical Analysis NINTH EDITION Richard L. Burden.pdf"),
                    895, "c5af17c41cc4049f9b3f028ad513182d316d6d1c7a1d683250fb18bda2e48d6d",
                    List.of(20, 50, 100, 500, 895)),
            new Book("subconjuntos-poda",
                    Path.of("C:/Users/MARCOS MOREIRA/Downloads/"
                            + "De_los_subconjuntos_a_la_poda.pdf"),
                    118, "e1d79ff6607cedbd0381ccf9ce8f472e542e5013588e6ca8ed3055852022fccf",
                    List.of(14, 19, 49, 54, 73, 82, 93, 97, 102)),
            new Book("limite-notable",
                    Path.of("C:/Users/MARCOS MOREIRA/Downloads/"
                            + "Demostracion_limite_notable.pdf"),
                    3, "dc069b1a39d0f18a36e5cc1138aeabfb10cb1c2b875f10c4ad5d7e2bdfc1fd92",
                    List.of(1, 2, 3)),
            new Book("informacion-actua",
                    Path.of("C:/Users/MARCOS MOREIRA/Downloads/"
                            + "Cuando_la_informacion_empieza_a_actuar.pdf"),
                    33, "c85d1b358d4458698856bf8ec89aaeea118b670e064dab0241b9d293ba3a048e",
                    List.of(1, 17, 33)));

    @Test
    void validatesSourcesAndProducesComparableSampleEvidence() throws Exception {
        Files.createDirectories(OUTPUT);
        PdfBoxRenderEngine renderer = new PdfBoxRenderEngine();
        PopplerPdfNativePageExtractor nativeExtractor = new PopplerPdfNativePageExtractor();
        PdfNativeTextQualityAssessor quality = new PdfNativeTextQualityAssessor();
        ArrayList<String> reports = new ArrayList<>();
        for (Book book : BOOKS) {
            assertTrue(Files.isRegularFile(book.path), "Falta " + book.path);
            assertEquals(book.sha256, sha256(book.path), book.id);
            try (var document = Loader.loadPDF(book.path.toFile())) {
                assertEquals(book.pages, document.getNumberOfPages(), book.id);
            }
            Path directory = OUTPUT.resolve(book.id);
            Files.createDirectories(directory);
            for (int page : book.samples) {
                PdfPageRenderResult rendered = renderer.renderPage(new PdfPageRenderRequest(
                        book.path, page, 144, 24_000_000L, Color.WHITE, true));
                BufferedImage image = rendered.image();
                Graphics2D graphics = image.createGraphics();
                try {
                    graphics.setColor(new Color(161, 98, 7));
                    graphics.setStroke(new BasicStroke(5f));
                    graphics.drawRect(3, 3, image.getWidth() - 7, image.getHeight() - 7);
                    graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
                    graphics.drawString(book.id + " - page " + page, 20, 34);
                } finally {
                    graphics.dispose();
                }
                ImageIO.write(image, "png",
                        directory.resolve("page-%04d-overlay.png".formatted(page)).toFile());
                PdfTextLayer nativeLayer = nativeExtractor.extract(book.path, page);
                PdfNativeTextQualityReport report = quality.assess(nativeLayer);
                reports.add("{\"book\":\"" + book.id + "\",\"page\":" + page
                        + ",\"nativeQuality\":\"" + report.quality() + "\",\"score\":"
                        + String.format(java.util.Locale.ROOT, "%.4f", report.score())
                        + ",\"regions\":" + report.regions().size() + "}");
            }
        }
        Files.writeString(OUTPUT.resolve("sample-report.json"),
                "{\"schemaVersion\":1,\"cases\":[" + String.join(",", reports) + "]}",
                StandardCharsets.UTF_8);
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(path)) {
            byte[] buffer = new byte[256 * 1024];
            for (int read; (read = input.read(buffer)) >= 0; ) {
                if (read > 0) digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private record Book(String id, Path path, int pages, String sha256, List<Integer> samples) {
    }
}
