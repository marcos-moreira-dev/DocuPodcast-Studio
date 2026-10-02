package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.BuildPdfOcrTextLayerUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextLayerBlockMapper;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparePdfPageRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparePdfPageUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfWorkspaceRef;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDocumentManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.process.DefaultExternalProcessRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Long, explicit gate for all 1,237 real pages. The originals are referenced
 * in place, never copied or modified, and only one page is prepared at a time.
 */
@EnabledIfSystemProperty(named = "docupodcast.pdf.fullCorpus", matches = "true")
final class PdfRealBooksFullPreparationTest {
    private static final Path OUTPUT =
            Path.of("target/pdf-real-corpus").toAbsolutePath().normalize();
    private static final List<Book> BOOKS = List.of(
            new Book("kress",
                    Path.of("C:/Users/MARCOS MOREIRA/Desktop/Numerical Analysis - Rainer Kress.pdf"),
                    342, "2631606a9815ebd76573160f825d6be0e95fd35c3e731fa0abd52671cbf20cdf"),
            new Book("burden",
                    Path.of("C:/Users/MARCOS MOREIRA/Desktop/"
                            + "Numerical Analysis NINTH EDITION Richard L. Burden.pdf"),
                    895, "c5af17c41cc4049f9b3f028ad513182d316d6d1c7a1d683250fb18bda2e48d6d"));

    @TempDir
    Path temp;

    @Test
    void preparesEveryRealPageWithTheCanonicalNativeFirstPipeline() throws Exception {
        Path tesseract = applicationRoot().resolve("tools/tesseract/bin/tesseract.exe");
        assertTrue(Files.isRegularFile(tesseract),
                "Falta Tesseract administrado: " + tesseract);
        Files.createDirectories(OUTPUT);
        long started = System.nanoTime();
        int completed = 0;
        int ocrPages = 0;
        int uncertainRegions = 0;
        ArrayList<String> failures = new ArrayList<>();

        for (Book book : BOOKS) {
            assertTrue(Files.isRegularFile(book.source), "Falta " + book.source);
            assertEquals(book.sha256, sha256(book.source), book.id);
            Path root = Files.createDirectories(temp.resolve(book.id));
            JsonPreparedPdfDocumentRepository repository =
                    new JsonPreparedPdfDocumentRepository();
            Instant now = Instant.now();
            repository.initialize(root, new PdfDocumentManifest(
                    PdfDocumentManifest.CURRENT_SCHEMA_VERSION,
                    book.id, book.source.getFileName().toString(), book.sha256,
                    book.pages, "pdf-v3-real-corpus", now, now));
            PreparedPdfWorkspaceRef workspace =
                    new PreparedPdfWorkspaceRef(root, book.source, book.sha256);
            OperationalSettings.OcrSettings ocrSettings =
                    new OperationalSettings.OcrSettings(
                            "managed-local", tesseract.toString(), "spa+eng",
                            216, 600, true, "");
            PreparePdfPageUseCase prepare = new PreparePdfPageUseCase(
                    new BuildPdfOcrTextLayerUseCase(new TesseractPdfOcrEngine(
                            new PdfBoxRenderEngine(), new DefaultExternalProcessRunner(),
                            tesseract::toString)),
                    new PdfTextLayerBlockMapper(), () -> ocrSettings,
                    repository, new PopplerPdfNativePageExtractor());
            for (int page = 1; page <= book.pages; page++) {
                var result = prepare.execute(new PreparePdfPageRequest(
                        workspace, root.resolve("cache"), page, false, null));
                if (!result.succeeded() || result.preparedPage() == null) {
                    failures.add(book.id + ":" + page + "=" + result.issues());
                } else {
                    completed++;
                    if (result.preparedPage().preparationMetrics().ocrUsed()) ocrPages++;
                    uncertainRegions += (int) result.preparedPage().regions().stream()
                            .filter(region -> region.effectiveNarratability()
                                    == PdfNarratability.UNCERTAIN)
                            .count();
                }
                if (page % 10 == 0 || page == book.pages) {
                    writeProgress(completed, ocrPages, uncertainRegions, failures,
                            (System.nanoTime() - started) / 1_000_000L);
                }
            }
            assertEquals(book.sha256, sha256(book.source),
                    "El libro original cambió durante la preparación");
        }

        writeProgress(completed, ocrPages, uncertainRegions, failures,
                (System.nanoTime() - started) / 1_000_000L);
        assertEquals(1_237, completed, () -> String.join(System.lineSeparator(), failures));
        assertTrue(failures.isEmpty(), () -> String.join(System.lineSeparator(), failures));
    }

    private static void writeProgress(int completed, int ocrPages,
                                      int uncertainRegions, List<String> failures,
                                      long elapsedMillis) throws Exception {
        Files.writeString(OUTPUT.resolve("full-corpus-report.json"),
                "{\"schemaVersion\":1,\"pagesCompleted\":" + completed
                        + ",\"ocrPages\":" + ocrPages
                        + ",\"uncertainRegions\":" + uncertainRegions
                        + ",\"elapsedMillis\":" + elapsedMillis
                        + ",\"failures\":" + failures.size() + "}",
                StandardCharsets.UTF_8);
    }

    private static Path applicationRoot() {
        String configured = System.getProperty("docupodcast.app.root");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("DOCUPODCAST_APP_ROOT");
        }
        return Path.of(configured == null || configured.isBlank() ? "." : configured)
                .toAbsolutePath().normalize();
    }

    private static String sha256(Path source) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(source)) {
            byte[] buffer = new byte[256 * 1024];
            for (int read; (read = input.read(buffer)) >= 0; ) {
                if (read > 0) digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest()).toLowerCase(Locale.ROOT);
    }

    private record Book(String id, Path source, int pages, String sha256) {
    }
}
