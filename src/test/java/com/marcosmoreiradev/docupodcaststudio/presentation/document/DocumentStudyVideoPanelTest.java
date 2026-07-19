package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentMascotPosition;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentParagraphVisualAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentVisualSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentStudyVideoPanelTest {
    @Test
    void copyingImportedImagePreservesDestinationDrawingAndMascot() {
        DocumentParagraphVisualAssignment source = assignment(
                "B0001", "IMPORTED-A", "DRAWN-A", "ink/a.json",
                DocumentVisualSource.IMPORTED, "MASCOT-A", DocumentMascotPosition.BOTTOM_LEFT);
        DocumentParagraphVisualAssignment destination = assignment(
                "B0002", "IMPORTED-B", "DRAWN-B", "ink/b.json",
                DocumentVisualSource.DRAWN, "MASCOT-B", DocumentMascotPosition.BOTTOM_RIGHT)
                .withSubtitle("Subtitulo propio")
                .withIllustrationOnly(true);

        DocumentParagraphVisualAssignment copied =
                DocumentStudyVideoPanel.copyActiveImageReference(source, destination);

        assertEquals("B0002", copied.blockId());
        assertEquals(DocumentVisualSource.IMPORTED, copied.activeSource());
        assertEquals("IMPORTED-A", copied.importedImageAssetId());
        assertEquals("DRAWN-B", copied.drawnImageAssetId());
        assertEquals("ink/b.json", copied.drawnStateRelativePath());
        assertEquals("MASCOT-B", copied.mascotAssetId());
        assertEquals(DocumentMascotPosition.BOTTOM_RIGHT, copied.mascotPosition());
        assertEquals("Subtitulo propio", copied.subtitle());
        assertTrue(copied.illustrationOnly());
        assertTrue(DocumentStudyVideoPanel.sameActiveImageReference(source, copied));
    }

    @Test
    void copyingDrawingPreservesDestinationImportedImageAndMascot() {
        DocumentParagraphVisualAssignment source = assignment(
                "B0001", "IMPORTED-A", "DRAWN-A", "ink/a.json",
                DocumentVisualSource.DRAWN, "MASCOT-A", DocumentMascotPosition.BOTTOM_LEFT);
        DocumentParagraphVisualAssignment destination = assignment(
                "B0002", "IMPORTED-B", "DRAWN-B", "ink/b.json",
                DocumentVisualSource.IMPORTED, "MASCOT-B", DocumentMascotPosition.BOTTOM_RIGHT);

        DocumentParagraphVisualAssignment copied =
                DocumentStudyVideoPanel.copyActiveImageReference(source, destination);

        assertEquals(DocumentVisualSource.DRAWN, copied.activeSource());
        assertEquals("IMPORTED-B", copied.importedImageAssetId());
        assertEquals("DRAWN-A", copied.drawnImageAssetId());
        assertEquals("ink/a.json", copied.drawnStateRelativePath());
        assertEquals("MASCOT-B", copied.mascotAssetId());
        assertEquals(DocumentMascotPosition.BOTTOM_RIGHT, copied.mascotPosition());
        assertTrue(DocumentStudyVideoPanel.sameActiveImageReference(source, copied));
    }

    @Test
    void mascotReferenceIncludesAssetAndPosition() {
        DocumentParagraphVisualAssignment left = assignment(
                "B0001", "", "", "", DocumentVisualSource.NONE,
                "MASCOT-A", DocumentMascotPosition.BOTTOM_LEFT);
        DocumentParagraphVisualAssignment same = assignment(
                "B0002", "", "", "", DocumentVisualSource.NONE,
                "MASCOT-A", DocumentMascotPosition.BOTTOM_LEFT);
        DocumentParagraphVisualAssignment otherPosition = assignment(
                "B0003", "", "", "", DocumentVisualSource.NONE,
                "MASCOT-A", DocumentMascotPosition.BOTTOM_RIGHT);

        assertTrue(DocumentStudyVideoPanel.sameMascotReference(left, same));
        assertTrue(!DocumentStudyVideoPanel.sameMascotReference(left, otherPosition));
    }

    private static DocumentParagraphVisualAssignment assignment(
            String blockId,
            String imported,
            String drawn,
            String state,
            DocumentVisualSource active,
            String mascot,
            DocumentMascotPosition position) {
        return new DocumentParagraphVisualAssignment(
                blockId, "fingerprint-" + blockId, imported, drawn, state, active, mascot, position);
    }
}
