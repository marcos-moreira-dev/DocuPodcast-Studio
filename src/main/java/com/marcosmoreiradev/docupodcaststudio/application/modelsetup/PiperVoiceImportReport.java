package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.nio.file.Path;
import java.util.List;

/** Result of importing a local lightweight voice model into the app-controlled Piper voice folder. */
public record PiperVoiceImportReport(
        boolean success,
        Path sourceFolder,
        Path targetFolder,
        String primaryVoiceFileName,
        ModelInspectionResult inspection,
        List<String> copiedFiles,
        String userMessage
) {
    public PiperVoiceImportReport {
        primaryVoiceFileName = primaryVoiceFileName == null ? "" : primaryVoiceFileName.strip();
        copiedFiles = List.copyOf(copiedFiles == null ? List.of() : copiedFiles);
        userMessage = userMessage == null ? "" : userMessage.strip();
    }

    public static PiperVoiceImportReport failed(Path sourceFolder, Path targetFolder, String userMessage) {
        return new PiperVoiceImportReport(false, sourceFolder, targetFolder, "", null, List.of(), userMessage);
    }
}
