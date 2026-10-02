package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.nio.file.Path;
import java.util.List;

/** Port for reading native PDF bookmarks without introducing block documents. */
@FunctionalInterface
public interface PdfBookmarkOutlineReader {
    PdfBookmarkOutlineReader NONE = sourcePdf -> List.of();

    List<DocumentOutlineHint> hintsFor(Path sourcePdf);
}
