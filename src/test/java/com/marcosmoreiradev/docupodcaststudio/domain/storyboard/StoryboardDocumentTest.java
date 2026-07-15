package com.marcosmoreiradev.docupodcaststudio.domain.storyboard;

import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StoryboardDocumentTest {
    @Test
    void createsScenesFromScriptAndStartsWithoutImages() {
        NarrationScriptDocument script = script();
        StoryboardDocument storyboard = StoryboardDocument.createForScript(script);

        List<StoryboardScene> scenes = storyboard.scenesFor(script);

        assertEquals(2, scenes.size());
        assertEquals("SEG-001", scenes.get(0).segmentId());
        assertTrue(!scenes.get(0).hasImage());
    }

    @Test
    void bindingAssociatesImageWithScene() {
        NarrationScriptDocument script = script();
        StoryboardDocument storyboard = StoryboardDocument.createForScript(script)
                .withBinding(StoryboardBinding.of("STB-001", "SEG-001", "IMG-001", "Escena inicial"));

        StoryboardScene first = storyboard.scenesFor(script).get(0);

        assertTrue(first.hasImage());
        assertEquals("IMG-001", first.imageAssetId());
        assertEquals("Escena inicial", first.caption());
    }

    @Test
    void rejectsTwoBindingsForSameSegment() {
        assertThrows(IllegalArgumentException.class, () -> new StoryboardDocument(
                "STORYBOARD-TEST",
                "Test",
                "SCRIPT-001",
                List.of(
                        StoryboardBinding.of("STB-001", "SEG-001", "IMG-001", "Uno"),
                        StoryboardBinding.of("STB-002", "SEG-001", "IMG-002", "Dos")
                ),
                Map.of(),
                java.time.Instant.now(),
                java.time.Instant.now(),
                ""
        ));
    }

    private static NarrationScriptDocument script() {
        return NarrationScriptDocument.create("Guion", "es", "Word", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Intro", "Texto uno", List.of("B001")),
                NarrationSegment.of("SEG-002", NarrationSegmentType.PARAGRAPH, "Cierre", "Texto dos", List.of("B002"))
        ));
    }
}
