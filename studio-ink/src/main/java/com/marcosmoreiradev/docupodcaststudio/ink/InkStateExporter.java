package com.marcosmoreiradev.docupodcaststudio.ink;

import java.io.IOException;
import java.nio.file.Path;

@FunctionalInterface
public interface InkStateExporter<S> {
    Path export(S state, Path destination, DrawingExportProfile profile) throws IOException;
}
