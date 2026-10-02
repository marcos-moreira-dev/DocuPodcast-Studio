package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import java.nio.file.Path;

/** One rendered technical-problem solution page to include in a batch PDF export. */
public record StudyProblemPdfPage(String title, Path imagePath, String canvasStateJson) {
    public StudyProblemPdfPage(String title, Path imagePath) { this(title, imagePath, ""); }
    public StudyProblemPdfPage {
        title = title == null || title.isBlank() ? "Problema tecnico" : title.strip();
        canvasStateJson = canvasStateJson == null ? "" : canvasStateJson;
    }
}
