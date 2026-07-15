package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.nio.file.Path;
import java.util.List;

/** Language data readiness for a resolved Tesseract runtime. */
public record TesseractLanguageDiscovery(
        List<String> requestedLanguages,
        List<String> missingLanguages,
        List<Path> checkedDirectories
) {
    public TesseractLanguageDiscovery {
        requestedLanguages = requestedLanguages == null ? List.of() : List.copyOf(requestedLanguages);
        missingLanguages = missingLanguages == null ? List.of() : List.copyOf(missingLanguages);
        checkedDirectories = checkedDirectories == null ? List.of() : List.copyOf(checkedDirectories);
    }

    public boolean ready() {
        return !requestedLanguages.isEmpty() && missingLanguages.isEmpty();
    }

    public String missingLabel() {
        return String.join(", ", missingLanguages);
    }
}
