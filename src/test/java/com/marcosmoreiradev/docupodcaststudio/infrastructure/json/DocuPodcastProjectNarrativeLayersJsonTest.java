package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.document.TextAnchorStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocuPodcastProjectNarrativeLayersJsonTest {
    @Test
    void writesAndReadsNarrativeLayerAssignmentsWithoutTouchingWordText() throws Exception {
        NarrativeLayerAssignment assignment = new NarrativeLayerAssignment(
                "LYR-001",
                NarrativeLayerKind.VOICE,
                new ScriptTextRange("SEG-001", 3, 32),
                new DocumentTextRange("BLK-001", 9, 38),
                "VOC-PEPITA",
                "Pepita",
                "Voz de personaje asignada desde el documento renderizado"
        );
        DocuPodcastProject project = DocuPodcastProject.createNew("Obra")
                .withNarrativeLayerAssignment(assignment);

        String json = new DocuPodcastProjectJsonWriter().write(project);
        assertTrue(json.contains("\"narrativeLayers\""));
        assertTrue(json.contains("\"sourceBlockId\": \"BLK-001\""));
        assertTrue(json.contains("\"textAnchor\""));
        assertTrue(json.contains("\"confidence\": \"LOW\""));
        assertTrue(json.contains("\"status\": \"NEEDS_REVIEW\""));
        assertTrue(json.contains("\"targetId\": \"VOC-PEPITA\""));

        DocuPodcastProject opened = new DocuPodcastProjectJsonReader().read(json);
        NarrativeLayerAssignment restored = opened.narrativeLayerAssignments().get(0);
        assertEquals("LYR-001", restored.id());
        assertEquals(NarrativeLayerKind.VOICE, restored.kind());
        assertEquals("SEG-001", restored.textRange().segmentId());
        assertEquals(3, restored.textRange().startOffset());
        assertEquals(32, restored.textRange().endOffset());
        assertEquals("BLK-001", restored.documentRange().blockId());
        assertEquals(9, restored.documentRange().startOffset());
        assertEquals(38, restored.documentRange().endOffset());
        assertTrue(restored.hasTextAnchor());
        assertEquals("ANCH-LYR-001", restored.textAnchor().id());
        assertEquals(TextAnchorStatus.NEEDS_REVIEW, restored.textAnchor().status());
    }

    @Test
    void bridgeImageLayerRoundTripsInProjectJson() throws Exception {
        NarrativeLayerAssignment assignment = new NarrativeLayerAssignment(
                "LYR-BRIDGE-001",
                NarrativeLayerKind.BRIDGE_IMAGE,
                new ScriptTextRange("SEG-001", 0, 18),
                new DocumentTextRange("BLK-001", 0, 18),
                "IMG-BRIDGE",
                "Puente visual",
                "Prompt puente");
        DocuPodcastProject project = DocuPodcastProject.createNew("Video")
                .withNarrativeLayerAssignment(assignment);

        String json = new DocuPodcastProjectJsonWriter().write(project);
        DocuPodcastProject opened = new DocuPodcastProjectJsonReader().read(json);

        assertTrue(json.contains("\"kind\": \"BRIDGE_IMAGE\""));
        assertEquals(NarrativeLayerKind.BRIDGE_IMAGE, opened.narrativeLayerAssignments().getFirst().kind());
        assertEquals("IMG-BRIDGE", opened.narrativeLayerAssignments().getFirst().targetId());
        assertEquals("Prompt puente", opened.narrativeLayerAssignments().getFirst().notes());
    }
}
