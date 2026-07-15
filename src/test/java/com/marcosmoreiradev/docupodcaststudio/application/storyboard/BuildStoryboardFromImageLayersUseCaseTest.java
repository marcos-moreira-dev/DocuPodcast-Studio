package com.marcosmoreiradev.docupodcaststudio.application.storyboard;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildStoryboardFromImageLayersUseCaseTest {
    @Test
    void buildsStoryboardBindingsFromRealImageLayersAndAllowsImageReuse() {
        NarrationScriptDocument script = script();
        ProjectAssetCatalog assets = new ProjectAssetCatalog(List.of(image("IMG-001")));
        List<NarrativeLayerAssignment> layers = List.of(
                imageLayer("LAY-001", "SEG-001", "IMG-001"),
                imageLayer("LAY-002", "SEG-002", "IMG-001")
        );

        StoryboardDocument storyboard = new BuildStoryboardFromImageLayersUseCase()
                .build(script, StoryboardDocument.createForScript(script), assets, layers);

        assertEquals(2, storyboard.bindingCount());
        assertEquals(2, storyboard.bindingsForImage("IMG-001").size());
        assertEquals("spoken-segment", storyboard.bindingForSegment("SEG-001").orElseThrow().metadata().get("duration"));
    }

    @Test
    void ignoresImageLayersWithoutRealVisualAsset() {
        NarrationScriptDocument script = script();
        StoryboardDocument storyboard = new BuildStoryboardFromImageLayersUseCase()
                .build(script, StoryboardDocument.createForScript(script), ProjectAssetCatalog.empty(),
                        List.of(imageLayer("LAY-001", "SEG-001", "IMG-MISSING")));

        assertTrue(storyboard.bindings().isEmpty());
    }

    private static NarrativeLayerAssignment imageLayer(String id, String segmentId, String imageAssetId) {
        return new NarrativeLayerAssignment(
                id,
                NarrativeLayerKind.IMAGE,
                new ScriptTextRange(segmentId, 0, 10),
                imageAssetId,
                "Imagen de apoyo",
                "capa de storyboard"
        );
    }

    private static ProjectAssetReference image(String id) {
        return new ProjectAssetReference(id, ProjectAssetKind.IMAGE, "Imagen", "media/images/" + id + ".png", "image/png", "storyboard", "", "");
    }

    private static NarrationScriptDocument script() {
        return NarrationScriptDocument.create("Narración", "es", "Documento", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Uno", "Texto uno", List.of("B001")),
                NarrationSegment.of("SEG-002", NarrationSegmentType.PARAGRAPH, "Dos", "Texto dos", List.of("B002"))
        ));
    }
}
