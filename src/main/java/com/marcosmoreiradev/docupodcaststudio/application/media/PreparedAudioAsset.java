package com.marcosmoreiradev.docupodcaststudio.application.media;

import java.nio.file.Path;
import java.util.Objects;

/** Project-local audio prepared in a temporary location until the owning track is saved. */
public record PreparedAudioAsset(
        String token,
        Path pendingPath,
        String sourceDisplayName,
        double durationSeconds,
        boolean normalized
) {
    public PreparedAudioAsset {
        token = Objects.requireNonNull(token, "token").strip();
        pendingPath = Objects.requireNonNull(pendingPath, "pendingPath").toAbsolutePath().normalize();
        sourceDisplayName = Objects.requireNonNullElse(sourceDisplayName, "audio").strip();
        if (token.isBlank() || durationSeconds <= 0.0) {
            throw new IllegalArgumentException("Prepared audio requires token and positive duration");
        }
    }
}
