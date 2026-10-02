package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.*;
import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

final class PdfReadingPreferencesTest {
    @TempDir Path temp;

    @Test void directReadingBypassesInstalledSemanticReaderAndReusesCache() throws Exception {
        var repository = new InMemoryPreparedPdfDocumentRepository();
        var workspace = PreparedPdfTestFixtures.workspace(repository, temp, 1);
        var preferences = new PdfReadingPreferencesUseCase(repository);
        preferences.update(workspace, PdfReadingStrategy.NATIVE_TEXT);
        AtomicInteger extractions = new AtomicInteger();
        var semantic = new AnalyzePdfPageSemanticallyUseCase(new PdfRenderEngine() {
            public PdfDocumentInfo inspect(Path path, PdfOpenOptions options) { throw new AssertionError("No rendering"); }
            public PdfPageRenderResult renderPage(PdfPageRenderRequest request) { throw new AssertionError("No semantic rendering"); }
            public PdfPageRenderResult renderCrop(PdfCropRenderRequest request) { throw new AssertionError("No crop"); }
        }, new MediaCapabilityService(new MediaEnginePlatform(null, null, null, null, null, null, null, null),
                new LocalResourceScheduler(java.util.Map.of())), new BlockPdfSemanticPageResponseParser());
        var prepare = new PreparePdfPageUseCase(new BuildPdfOcrTextLayerUseCase(request -> {
            throw new AssertionError("Direct reading must not start OCR");
        }), null, null, repository, (path, number) -> {
            extractions.incrementAndGet();
            return layer(number);
        }, semantic);
        var request = new PreparePdfPageRequest(workspace, null, 1, false, null);
        var first = prepare.execute(request);
        assertTrue(first.succeeded(), first.issues().toString());
        assertTrue(prepare.execute(request).succeeded());
        assertEquals(1, extractions.get());
        assertEquals(PdfReadingStrategy.NATIVE_TEXT, preferences.read(workspace).readingStrategy());
        assertTrue(first.preparedPage().regions().getFirst().attributes().containsKey("lineBboxes"));
        preferences.update(workspace, PdfReadingStrategy.NATIVE_WITH_OCR);
        assertTrue(prepare.execute(request).succeeded());
        assertEquals(2, extractions.get(), "Changed strategy must not reuse an incompatible signature");
    }

    @Test void scannedPageDoesNotFallThroughToOcrInDirectMode() throws Exception {
        var repository = new InMemoryPreparedPdfDocumentRepository();
        var workspace = PreparedPdfTestFixtures.workspace(repository, temp, 1);
        new PdfReadingPreferencesUseCase(repository).update(workspace, PdfReadingStrategy.NATIVE_TEXT);
        var prepare = new PreparePdfPageUseCase(new BuildPdfOcrTextLayerUseCase(request -> {
            throw new AssertionError("OCR was not requested");
        }), null, null, repository, (path, number) -> new PdfTextLayer(number,
                PdfTextLayerOrigin.UNAVAILABLE, List.of(), List.of()));
        var result = prepare.execute(new PreparePdfPageRequest(workspace, null, 1, false, null));
        assertFalse(result.succeeded());
        assertTrue(result.issues().getFirst().message().contains("Lectura con reconocimiento"));
        assertTrue(repository.loadPages(temp).isEmpty());
    }

    @Test void preferenceRoundTripAndLegacyManifestAreCompatible() throws Exception {
        var repository = new JsonPreparedPdfDocumentRepository();
        var manifest = new PdfDocumentManifest(3, "Prueba", "source.pdf", PreparedPdfTestFixtures.SHA,
                1, "pdf-v2", Instant.EPOCH, Instant.EPOCH);
        repository.initialize(temp, manifest.withReadingPreferences(PdfReadingStrategy.NATIVE_TEXT, PdfNativeTextProvider.POPPLER));
        var loaded = repository.loadManifest(temp).orElseThrow();
        assertEquals(PdfReadingStrategy.NATIVE_TEXT, loaded.readingStrategy());
        assertEquals(PdfNativeTextProvider.POPPLER, loaded.nativeTextProvider());
        repository.initialize(temp, manifest);
        assertEquals(PdfReadingStrategy.NATIVE_TEXT, repository.loadManifest(temp).orElseThrow().readingStrategy());
        Path file = temp.resolve("document/manifest.json");
        Files.writeString(file, Files.readString(file).replaceAll("(?m)^.*\"(?:readingStrategy|nativeTextProvider)\".*\\R", ""));
        assertEquals(PdfReadingStrategy.SEMANTIC, repository.loadManifest(temp).orElseThrow().readingStrategy());
    }

    private static PdfTextLayer layer(int page) {
        var box = new PdfPageRegion(page, 20, 30, 580, 70, 612, 792);
        return new PdfTextLayer(page, PdfTextLayerOrigin.NATIVE_BBOX,
                List.of(new PdfTextLine(page, "Este párrafo contiene texto nativo fiable para estudiar sin interpretar imágenes.",
                        box, List.of(), 1.0)), List.of());
    }
}
