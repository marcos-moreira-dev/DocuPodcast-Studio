package com.marcosmoreiradev.docupodcaststudio.application.audio;

import java.io.IOException;
import java.nio.file.Path;
import java.util.OptionalDouble;

/** Application port for measuring local audio clips without coupling use cases to a codec adapter. */
public interface AudioDurationProbe {
    double durationSeconds(Path audioFile) throws IOException;

    default OptionalDouble tryDurationSeconds(Path audioFile) {
        try {
            return OptionalDouble.of(durationSeconds(audioFile));
        } catch (IOException | RuntimeException ex) {
            return OptionalDouble.empty();
        }
    }
}
