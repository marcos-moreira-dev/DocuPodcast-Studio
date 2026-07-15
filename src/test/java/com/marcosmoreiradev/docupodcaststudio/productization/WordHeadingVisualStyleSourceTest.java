package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** DOC-WORD-STYLE1: Word headings should look distinct from prose inside the reader. */
final class WordHeadingVisualStyleSourceTest {
    @Test
    void documentPageCssHighlightsTitlesHeadingsAndSubheadingsWithSoftBrownTypography() throws IOException {
        String css = Files.readString(Path.of("src/main/resources/css/document/document-page.css"));
        assertTrue(css.contains(".document-block-title .document-block-text"));
        assertTrue(css.contains(".document-block-heading .document-block-text"));
        assertTrue(css.contains(".document-block-subheading .document-block-text"));
        assertTrue(css.contains("#6A4E3B"));
        assertTrue(css.contains("#745842"));
        assertTrue(css.contains("#826450"));
    }
}
