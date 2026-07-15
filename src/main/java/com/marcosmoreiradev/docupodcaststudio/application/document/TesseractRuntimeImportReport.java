package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.nio.file.Path;
import java.util.List;

/** Result of importing a local Tesseract portable folder into the app runtime. */
public record TesseractRuntimeImportReport(
        boolean success,
        Path sourceFolder,
        Path targetRoot,
        Path executable,
        List<String> copiedFiles,
        List<String> missingLanguages,
        String userMessage
) {
    public TesseractRuntimeImportReport {
        copiedFiles = copiedFiles == null ? List.of() : List.copyOf(copiedFiles);
        missingLanguages = missingLanguages == null ? List.of() : List.copyOf(missingLanguages);
        userMessage = userMessage == null ? "" : userMessage.strip();
    }

    public static TesseractRuntimeImportReport failed(Path sourceFolder, Path targetRoot, String message) {
        return new TesseractRuntimeImportReport(false, sourceFolder, targetRoot, null,
                List.of(), List.of("spa", "eng"), message);
    }
}
