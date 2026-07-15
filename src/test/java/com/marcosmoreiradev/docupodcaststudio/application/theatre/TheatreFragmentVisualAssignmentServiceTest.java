package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TheatreFragmentVisualAssignmentServiceTest {
    @Test
    void assignsOrderedFragmentFilesToNarratableSegments() {
        NarrationScriptDocument script = NarrationScriptDocument.create("Demo", "es", "source.docx", List.of(
                segment("SEG-001", "Texto 1"),
                segment("SEG-002", "Texto 2"),
                segment("SEG-003", "Texto 3. Segunda oracion."),
                segment("SEG-004", "Texto 4"),
                segment("SEG-005", "Texto 5")));
        ProjectAssetCatalog assets = new ProjectAssetCatalog(List.of(
                image("IMG-004", "fragmento_01_escena1_hangar_presentacion.png"),
                image("IMG-002", "fragmento_00_presentacion_personajes.png"),
                image("IMG-003", "imagen_01_presentacion_personajes.png"),
                image("IMG-001", "fragmento_000_presentacion_personajes.png"),
                image("IMG-005", "fragmento_02_narrador_aerodromo.png")));

        List<NarrativeLayerAssignment> assignments = new TheatreFragmentVisualAssignmentService()
                .assign(script, assets, List.of());

        assertEquals(5, assignments.size());
        assertEquals("IMG-001", assignments.get(0).targetId());
        assertEquals("SEG-001", assignments.get(0).textRange().segmentId());
        assertEquals("IMG-002", assignments.get(1).targetId());
        assertEquals("SEG-002", assignments.get(1).textRange().segmentId());
        assertEquals("IMG-003", assignments.get(2).targetId());
        assertEquals("SEG-003", assignments.get(2).textRange().segmentId());
        assertEquals("IMG-004", assignments.get(3).targetId());
        assertEquals("SEG-004", assignments.get(3).textRange().segmentId());
        assertEquals("IMG-005", assignments.get(4).targetId());
        assertEquals("SEG-005", assignments.get(4).textRange().segmentId());
    }

    @Test
    void orderedFragmentsWinAndMarkdownFallbackFillsMissingSegments() {
        NarrationScriptDocument script = NarrationScriptDocument.create("Demo", "es", "source.docx", List.of(
                segment("SEG-001", "Texto 1"),
                segment("SEG-002", "Texto 2")));
        ProjectAssetCatalog assets = new ProjectAssetCatalog(List.of(
                image("IMG-001", "fragmento_000_presentacion_personajes.png")));
        NarrativeLayerAssignment fallback = new NarrativeLayerAssignment(
                "NLA-IMAGE-MD-INTERVENCION-2-1",
                NarrativeLayerKind.IMAGE,
                new ScriptTextRange("SEG-002", 0, 7),
                "IMG-MD",
                "Visual MD",
                "Visual desde teatro.md.");

        List<NarrativeLayerAssignment> assignments = new TheatreFragmentVisualAssignmentService()
                .assign(script, assets, List.of(fallback));

        assertEquals(2, assignments.size());
        assertEquals("IMG-001", assignments.get(0).targetId());
        assertEquals("IMG-MD", assignments.get(1).targetId());
    }

    private static NarrationSegment segment(String id, String text) {
        return NarrationSegment.of(id, NarrationSegmentType.PARAGRAPH, "", text, List.of(id.replace("SEG", "B")));
    }

    private static ProjectAssetReference image(String id, String displayName) {
        return new ProjectAssetReference(
                id,
                ProjectAssetKind.IMAGE,
                displayName,
                "media/images/" + id.toLowerCase(java.util.Locale.ROOT) + "-" + displayName,
                "image/png",
                "Imagen demo",
                "",
                "");
    }
}
