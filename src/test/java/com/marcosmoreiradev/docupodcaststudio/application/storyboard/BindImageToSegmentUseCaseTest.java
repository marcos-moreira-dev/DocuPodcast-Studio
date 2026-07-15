package com.marcosmoreiradev.docupodcaststudio.application.storyboard;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDisplayMode;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BindImageToSegmentUseCaseTest {
    @Test
    void bindsRegisteredImageToExistingSegment() {
        NarrationScriptDocument script = script();
        StoryboardDocument storyboard = StoryboardDocument.createForScript(script);
        ProjectAssetCatalog assets = new ProjectAssetCatalog(List.of(image("IMG-001")));

        StoryboardDocument updated = new BindImageToSegmentUseCase()
                .bind(storyboard, script, assets, "SEG-001", "IMG-001", "Mi imagen", StoryboardDisplayMode.FIT_CONTAIN);

        assertEquals(1, updated.bindingCount());
        assertTrue(updated.bindingForSegment("SEG-001").isPresent());
        assertEquals("IMG-001", updated.bindingForSegment("SEG-001").get().imageAssetId());
    }

    @Test
    void rejectsUnknownSegmentOrImage() {
        NarrationScriptDocument script = script();
        StoryboardDocument storyboard = StoryboardDocument.createForScript(script);
        ProjectAssetCatalog assets = new ProjectAssetCatalog(List.of(image("IMG-001")));

        assertThrows(IllegalArgumentException.class, () -> new BindImageToSegmentUseCase()
                .bind(storyboard, script, assets, "SEG-999", "IMG-001", "", StoryboardDisplayMode.FIT_CONTAIN));
        assertThrows(IllegalArgumentException.class, () -> new BindImageToSegmentUseCase()
                .bind(storyboard, script, assets, "SEG-001", "IMG-999", "", StoryboardDisplayMode.FIT_CONTAIN));
    }

    private static ProjectAssetReference image(String id) {
        return new ProjectAssetReference(id, ProjectAssetKind.IMAGE, "Imagen", "media/images/test.png", "image/png", "test", "", "");
    }

    private static NarrationScriptDocument script() {
        return NarrationScriptDocument.create("Guion", "es", "Word", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Intro", "Texto uno", List.of("B001"))
        ));
    }
}
