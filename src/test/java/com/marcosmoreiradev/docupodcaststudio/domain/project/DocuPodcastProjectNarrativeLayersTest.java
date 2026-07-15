package com.marcosmoreiradev.docupodcaststudio.domain.project;

import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class DocuPodcastProjectNarrativeLayersTest {
    @Test
    void keepsCompatibleLayersAndRejectsOverlappingPrimarySources() {
        NarrativeLayerAssignment voice = assignment("LYR-001", NarrativeLayerKind.VOICE, "VOC-PEPITA");
        NarrativeLayerAssignment emotion = assignment("LYR-002", NarrativeLayerKind.EMOTION, "STY-HAPPY");
        NarrativeLayerAssignment audio = assignment("LYR-003", NarrativeLayerKind.HUMAN_AUDIO, "AUD-HOMBRE");

        DocuPodcastProject project = DocuPodcastProject.createNew("Obra")
                .withNarrativeLayerAssignment(voice)
                .withNarrativeLayerAssignment(emotion);

        assertEquals(2, project.narrativeLayerAssignments().size());
        assertThrows(IllegalArgumentException.class, () -> project.withNarrativeLayerAssignment(audio));
    }

    @Test
    void canRemoveAssignmentBeforeReplacingPrimarySource() {
        DocuPodcastProject project = DocuPodcastProject.createNew("Obra")
                .withNarrativeLayerAssignment(assignment("LYR-001", NarrativeLayerKind.VOICE, "VOC-PEPITA"));

        DocuPodcastProject replaced = project.withoutNarrativeLayerAssignment("LYR-001")
                .withNarrativeLayerAssignment(assignment("LYR-002", NarrativeLayerKind.HUMAN_AUDIO, "AUD-LINEA"));

        assertEquals(1, replaced.narrativeLayerAssignments().size());
        assertEquals(NarrativeLayerKind.HUMAN_AUDIO, replaced.narrativeLayerAssignments().get(0).kind());
    }

    private static NarrativeLayerAssignment assignment(String id, NarrativeLayerKind kind, String targetId) {
        return new NarrativeLayerAssignment(
                id,
                kind,
                new ScriptTextRange("SEG-001", 0, 20),
                targetId,
                targetId,
                "");
    }
}
