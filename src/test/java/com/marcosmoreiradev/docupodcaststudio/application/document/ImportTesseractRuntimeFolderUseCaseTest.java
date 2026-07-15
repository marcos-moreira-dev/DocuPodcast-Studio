package com.marcosmoreiradev.docupodcaststudio.application.document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImportTesseractRuntimeFolderUseCaseTest {
    @TempDir
    Path tempDir;

    @Test
    void importsExecutableAndRequiredLanguagesIntoManagedRuntime() throws Exception {
        Path source = tempDir.resolve("portable").resolve("bin");
        Path tessdata = tempDir.resolve("portable").resolve("tessdata");
        Files.createDirectories(source);
        Files.createDirectories(tessdata);
        Files.writeString(source.resolve("tesseract.exe"), "fake exe");
        Files.writeString(source.resolve("libtesseract.dll"), "fake dll");
        Files.writeString(tessdata.resolve("spa.traineddata"), "spa");
        Files.writeString(tessdata.resolve("eng.traineddata"), "eng");
        Path appRoot = tempDir.resolve("app");

        TesseractRuntimeImportReport report = new ImportTesseractRuntimeFolderUseCase()
                .importFrom(tempDir.resolve("portable"), appRoot);

        assertTrue(report.success());
        assertTrue(Files.isRegularFile(appRoot.resolve("tools/tesseract/bin/tesseract.exe")));
        assertTrue(Files.isRegularFile(appRoot.resolve("tools/tesseract/bin/libtesseract.dll")));
        assertTrue(Files.isRegularFile(appRoot.resolve("tools/tesseract/tessdata/spa.traineddata")));
        assertTrue(Files.isRegularFile(appRoot.resolve("tools/tesseract/tessdata/eng.traineddata")));
        assertEquals("Tesseract OCR quedo importado con idiomas spa y eng dentro del programa.", report.userMessage());
    }

    @Test
    void reportsMissingLanguagesClearly() throws Exception {
        Path source = tempDir.resolve("portable").resolve("bin");
        Files.createDirectories(source);
        Files.writeString(source.resolve("tesseract.exe"), "fake exe");

        TesseractRuntimeImportReport report = new ImportTesseractRuntimeFolderUseCase()
                .importFrom(tempDir.resolve("portable"), tempDir.resolve("app"));

        assertFalse(report.success());
        assertEquals(java.util.List.of("spa", "eng"), report.missingLanguages());
        assertTrue(report.userMessage().contains("faltan idiomas OCR"));
    }
}
