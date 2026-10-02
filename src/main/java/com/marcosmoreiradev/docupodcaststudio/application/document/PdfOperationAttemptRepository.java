package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfOperationAttempt;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Persistence port for append-only PDF operation attempts. */
public interface PdfOperationAttemptRepository {
    List<PdfOperationAttempt> list(Path projectRoot) throws IOException;
    void save(Path projectRoot, PdfOperationAttempt attempt) throws IOException;

    static PdfOperationAttemptRepository disabled() {
        return new PdfOperationAttemptRepository() {
            @Override public List<PdfOperationAttempt> list(Path root) {
                return List.of();
            }
            @Override public void save(Path root, PdfOperationAttempt attempt) { }
        };
    }
}
