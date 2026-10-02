package com.marcosmoreiradev.docupodcaststudio.application.theatrepackage;

import java.io.IOException;
import java.nio.file.Path;

/** Application port used when a theatre refresh must restore a JSON pointer file. */
@FunctionalInterface
public interface AtomicJsonWriter {
    void write(Path target, String json) throws IOException;
}
