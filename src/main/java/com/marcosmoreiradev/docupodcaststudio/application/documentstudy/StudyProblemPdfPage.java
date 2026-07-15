package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import java.nio.file.Path;

/** One rendered technical-problem solution page to include in a batch PDF export. */
public record StudyProblemPdfPage(String title, Path imagePath) {
    public StudyProblemPdfPage {
        title = title == null || title.isBlank() ? "Problema tecnico" : title.strip();
    }
}
