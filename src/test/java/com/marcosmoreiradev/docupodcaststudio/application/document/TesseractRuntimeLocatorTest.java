package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TesseractRuntimeLocatorTest {
    @TempDir
    Path tempDir;

    @Test
    void resolvesConfiguredExecutableFromSettings() throws Exception {
        Path exe = tempDir.resolve("ocr").resolve("tesseract.exe");
        Files.createDirectories(exe.getParent());
        Files.writeString(exe, "fake");
        OperationalSettings settings = withOcrExecutable(exe.toString());

        TesseractToolDiscovery discovery = new TesseractRuntimeLocator().locate(settings, tempDir);

        assertTrue(discovery.ready());
        assertEquals(exe.toString(), discovery.command());
        assertEquals("settings", discovery.source());
    }

    @Test
    void resolvesManagedPortableTesseractUnderTools() throws Exception {
        Path exe = tempDir.resolve("tools").resolve("tesseract").resolve("bin").resolve("tesseract.exe");
        Files.createDirectories(exe.getParent());
        Files.writeString(exe, "fake");
        writeLanguages(tempDir.resolve("tools").resolve("tesseract").resolve("tessdata"));

        TesseractToolDiscovery discovery = new TesseractRuntimeLocator().locate(OperationalSettings.defaults(), tempDir);
        TesseractLanguageDiscovery languages = new TesseractRuntimeLocator().inspectLanguages(discovery, "spa+eng");

        assertTrue(discovery.ready());
        assertTrue(languages.ready());
        assertEquals(exe.toString(), discovery.command());
        assertEquals("managed-local", discovery.source());
    }

    @Test
    void reportsMissingLanguageDataSeparatelyFromExecutable() throws Exception {
        Path exe = tempDir.resolve("tools").resolve("tesseract").resolve("bin").resolve("tesseract.exe");
        Files.createDirectories(exe.getParent());
        Files.writeString(exe, "fake");

        TesseractToolDiscovery discovery = new TesseractRuntimeLocator().locate(OperationalSettings.defaults(), tempDir);
        TesseractLanguageDiscovery languages = new TesseractRuntimeLocator().inspectLanguages(discovery, "spa+eng");

        assertTrue(discovery.ready());
        assertFalse(languages.ready());
        assertEquals(java.util.List.of("spa", "eng"), languages.missingLanguages());
    }

    @Test
    void managedPortableTesseractWinsOverConfiguredExecutable() throws Exception {
        Path managed = tempDir.resolve("tools").resolve("tesseract").resolve("bin").resolve("tesseract.exe");
        Path configured = tempDir.resolve("custom").resolve("tesseract.exe");
        Files.createDirectories(managed.getParent());
        Files.createDirectories(configured.getParent());
        Files.writeString(managed, "managed");
        Files.writeString(configured, "custom");

        TesseractToolDiscovery discovery = new TesseractRuntimeLocator().locate(withOcrExecutable(configured.toString()), tempDir);

        assertTrue(discovery.ready());
        assertEquals(managed.toString(), discovery.command());
        assertEquals("managed-local", discovery.source());
    }

    @Test
    void reportsPathFallbackWhenNoRuntimeExists() {
        TesseractToolDiscovery discovery = new TesseractRuntimeLocator(false).locate(OperationalSettings.defaults(), tempDir);

        assertFalse(discovery.ready());
        assertEquals("tesseract", discovery.command());
        assertEquals("path-fallback", discovery.source());
    }

    private static OperationalSettings withOcrExecutable(String executable) {
        OperationalSettings defaults = OperationalSettings.defaults();
        return new OperationalSettings(
                defaults.readingDocument(),
                defaults.playbackBuffer(),
                defaults.tts(),
                defaults.video(),
                defaults.imageGeneration(),
                defaults.frameGeneration(),
                defaults.compute(),
                new OperationalSettings.OcrSettings("managed-local", executable, "spa+eng", 300, 180, true, ""),
                defaults.storage(),
                defaults.diagnostics());
    }

    private static void writeLanguages(Path tessdata) throws Exception {
        Files.createDirectories(tessdata);
        Files.writeString(tessdata.resolve("spa.traineddata"), "spa");
        Files.writeString(tessdata.resolve("eng.traineddata"), "eng");
    }
}
