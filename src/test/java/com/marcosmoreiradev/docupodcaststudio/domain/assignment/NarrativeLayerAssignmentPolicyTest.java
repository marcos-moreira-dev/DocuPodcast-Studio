package com.marcosmoreiradev.docupodcaststudio.domain.assignment;

import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NarrativeLayerAssignmentPolicyTest {
    @Test
    void primaryVoiceAndPrimaryAudioCannotOverlapSameRange() {
        NarrativeLayerAssignment voice = assignment("A1", NarrativeLayerKind.VOICE, 0, 20, "VOC-PEPITA");
        NarrativeLayerAssignment audio = assignment("A2", NarrativeLayerKind.HUMAN_AUDIO, 5, 18, "AUD-LINEA-01");

        assertFalse(NarrativeLayerAssignmentPolicy.compatible(voice, audio));
        assertEquals(1, NarrativeLayerAssignmentPolicy.conflicts(List.of(voice), audio).size());
    }

    @Test
    void emotionAndImageCanShareRangeWithVoice() {
        NarrativeLayerAssignment voice = assignment("A1", NarrativeLayerKind.VOICE, 0, 20, "VOC-PEPITA");
        NarrativeLayerAssignment emotion = assignment("A2", NarrativeLayerKind.EMOTION, 0, 20, "STY-HAPPY");
        NarrativeLayerAssignment image = assignment("A3", NarrativeLayerKind.IMAGE, 0, 20, "IMG-BOSQUE");

        assertTrue(NarrativeLayerAssignmentPolicy.compatible(voice, emotion));
        assertTrue(NarrativeLayerAssignmentPolicy.compatible(voice, image));
        assertTrue(NarrativeLayerAssignmentPolicy.conflicts(List.of(voice, emotion), image).isEmpty());
    }

    private static NarrativeLayerAssignment assignment(
            String id,
            NarrativeLayerKind kind,
            int startOffset,
            int endOffset,
            String targetId) {
        return new NarrativeLayerAssignment(
                id,
                kind,
                new ScriptTextRange("SEG-001", startOffset, endOffset),
                targetId,
                targetId,
                "");
    }
}
