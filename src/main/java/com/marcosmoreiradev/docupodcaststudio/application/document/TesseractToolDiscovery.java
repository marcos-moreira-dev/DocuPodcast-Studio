package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.nio.file.Path;
import java.util.List;

/** Resolved local Tesseract command and diagnostics for OCR. */
public record TesseractToolDiscovery(
        String command,
        Path executable,
        boolean ready,
        String source,
        Path expectedFolder,
        List<String> diagnostics
) {
    public TesseractToolDiscovery {
        command = command == null || command.isBlank() ? "tesseract" : command.strip();
        source = source == null || source.isBlank() ? "path-fallback" : source.strip();
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }
}
