package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prevents speaker prefixes in bold from being promoted to document subsections. */
final class DocxPartialBoldHeadingHf1SourceTest {
    @Test
    void docxImporterDistinguishesPartialBoldSpeakerPrefixFromStructuralBoldHeading() throws Exception {
        String importer = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/document/DocxDocumentImporter.java"));

        assertTrue(importer.contains("boolean partialBold = hasDescendant(paragraph, \"b\")"));
        assertTrue(importer.contains("boolean structurallyBold = isEffectivelyBoldParagraph(paragraph)"));
        assertTrue(importer.contains("typeFromStyle(styleId, styleName, text, numbered, structurallyBold)"));
        assertTrue(importer.contains("metadata.put(\"partialBold\", \"true\")"));
        assertTrue(importer.contains("isEffectivelyBoldParagraph"));
        assertFalse(importer.contains("typeFromStyle(styleId, styleName, text, numbered, bold)"),
                "partial bold prefixes must not be passed as structural heading evidence");
    }
}
