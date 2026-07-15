package com.marcosmoreiradev.docupodcaststudio.application.voice;

import java.nio.file.Path;
import java.util.Optional;

/** Result of deleting a managed voice sample. */
public record VoiceSampleDeleteResult(
        boolean deleted,
        Optional<Path> deletedFile,
        String message
) {
    public VoiceSampleDeleteResult {
        deletedFile = deletedFile == null ? Optional.empty() : deletedFile;
        message = message == null ? "" : message.strip();
    }

    public static VoiceSampleDeleteResult deleted(Path file) {
        return new VoiceSampleDeleteResult(true, Optional.of(file), "Muestra eliminada correctamente.");
    }

    public static VoiceSampleDeleteResult skipped(String message) {
        return new VoiceSampleDeleteResult(false, Optional.empty(), message);
    }
}
