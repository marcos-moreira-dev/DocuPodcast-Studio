package com.marcosmoreiradev.docupodcaststudio.application.voice;

/** Result of writing voices/voice-library.json. */
public record MaterializedVoiceLibrary(String relativePath, String displayName) {
    public MaterializedVoiceLibrary {
        relativePath = relativePath == null ? "" : relativePath.strip();
        displayName = displayName == null ? "" : displayName.strip();
        if (relativePath.isBlank()) {
            throw new IllegalArgumentException("relativePath is required");
        }
    }
}
