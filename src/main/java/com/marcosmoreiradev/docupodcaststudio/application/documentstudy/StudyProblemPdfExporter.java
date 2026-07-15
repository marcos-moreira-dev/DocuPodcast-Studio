package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Application boundary for composing saved study-problem solution images into a PDF. */
@FunctionalInterface
public interface StudyProblemPdfExporter {
    void export(List<StudyProblemPdfPage> pages, Path targetPdf) throws IOException;

    static StudyProblemPdfExporter unavailable() {
        return (pages, targetPdf) -> {
            throw new IOException("Exportador PDF de ejercicios no configurado.");
        };
    }
}
