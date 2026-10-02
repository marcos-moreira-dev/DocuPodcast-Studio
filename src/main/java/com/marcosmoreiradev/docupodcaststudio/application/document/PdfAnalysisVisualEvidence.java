package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Temporary PDF-specific visual evidence passed to a product-neutral engine. */
public record PdfAnalysisVisualEvidence(Path roiImage, Path markedPageImage)
        implements AutoCloseable {
    public PdfAnalysisVisualEvidence {
        roiImage = roiImage.toAbsolutePath().normalize();
        markedPageImage = markedPageImage.toAbsolutePath().normalize();
    }

    @Override
    public void close() {
        Path directory = roiImage.getParent();
        try {
            Files.deleteIfExists(roiImage);
            Files.deleteIfExists(markedPageImage);
            if (directory != null) Files.deleteIfExists(directory);
        } catch (IOException ignored) {
            // These are regenerable temporary inputs; cleanup is best effort.
        }
    }
}
