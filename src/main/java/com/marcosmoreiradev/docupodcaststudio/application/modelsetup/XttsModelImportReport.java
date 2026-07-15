package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.nio.file.Path;
import java.util.List;

/** Result of importing a user-provided advanced voice model folder into the application runtime layout. */
public record XttsModelImportReport(
        boolean success,
        Path sourceFolder,
        Path targetFolder,
        ModelInspectionResult inspection,
        List<String> copiedFiles,
        String userMessage
) {
    public XttsModelImportReport {
        copiedFiles = List.copyOf(copiedFiles == null ? List.of() : copiedFiles);
        userMessage = userMessage == null ? "" : userMessage.strip();
    }

    public static XttsModelImportReport failed(Path source, Path target, String message) {
        return new XttsModelImportReport(false, source, target, null, List.of(), message);
    }
}
