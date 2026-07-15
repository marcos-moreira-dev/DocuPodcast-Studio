package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.nio.file.Path;
import java.util.List;

public record FluxComponentImportReport(boolean success, Path source, List<Path> installedFiles, String userMessage) {
    public FluxComponentImportReport {
        installedFiles = List.copyOf(installedFiles == null ? List.of() : installedFiles);
        userMessage = userMessage == null ? "" : userMessage.strip();
    }
}
