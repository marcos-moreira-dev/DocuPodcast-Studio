package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Builds a PDF from ordered image files without exposing the PDF backend to presentation. */
public interface ImageFolderPdfBuilder {
    Path build(List<Path> imageFiles, Path targetPdf) throws IOException;
}
