package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentMediaRailVariantCategorySourceTest {
    @Test
    void altVariantBadgeShowsHumanReadableVisualCategory() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentMediaRailView.java"));

        assertTrue(source.contains("variantCategoryLabel"));
        assertTrue(source.contains("case OFFICIAL -> \"Usuario\""));
        assertTrue(source.contains("case GENERATED -> \"IA\""));
        assertTrue(source.contains("case DRAWN -> \"Lienzo\""));
    }
}
