package com.marcosmoreiradev.docupodcaststudio.application.narrative;

import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NarrativeVisualSlotPolicyTest {
    private final NarrativeVisualSlotPolicy policy = new NarrativeVisualSlotPolicy();

    @Test
    void replacesDuplicatedSlotOfSameRoleAndAllowsMainPlusBridge() {
        NarrativeLayerAssignment main = assignment("LYR-IMAGE-1", NarrativeLayerKind.IMAGE, "IMG-MAIN");
        NarrativeLayerAssignment oldBridge = assignment("LYR-BRIDGE-1", NarrativeLayerKind.BRIDGE_IMAGE, "IMG-OLD");
        NarrativeLayerAssignment newBridge = assignment("LYR-BRIDGE-2", NarrativeLayerKind.BRIDGE_IMAGE, "IMG-NEW");

        List<NarrativeLayerAssignment> updated = policy.replaceSlot(List.of(main, oldBridge), newBridge);

        assertEquals(2, updated.size());
        assertTrue(updated.stream().anyMatch(layer -> layer.kind() == NarrativeLayerKind.IMAGE
                && layer.targetId().equals("IMG-MAIN")));
        assertTrue(updated.stream().anyMatch(layer -> layer.kind() == NarrativeLayerKind.BRIDGE_IMAGE
                && layer.targetId().equals("IMG-NEW")));
        assertTrue(updated.stream().noneMatch(layer -> layer.targetId().equals("IMG-OLD")));
    }

    @Test
    void rejectsNonVisualSlotKinds() {
        NarrativeLayerAssignment voice = assignment("LYR-VOICE-1", NarrativeLayerKind.VOICE, "VOC-NARRATOR");

        assertThrows(IllegalArgumentException.class, () -> policy.replaceSlot(List.of(), voice));
    }

    private static NarrativeLayerAssignment assignment(String id, NarrativeLayerKind kind, String targetId) {
        return new NarrativeLayerAssignment(
                id,
                kind,
                new ScriptTextRange("SEG-001", 0, 20),
                targetId,
                targetId,
                "notes");
    }
}
