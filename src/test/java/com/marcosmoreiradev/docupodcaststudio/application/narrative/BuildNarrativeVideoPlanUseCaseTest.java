package com.marcosmoreiradev.docupodcaststudio.application.narrative;

import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetRole;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetSource;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildNarrativeVideoPlanUseCaseTest {
    @Test
    void addsSpokenMainImageFrameAndSilentBridgeFrame() {
        NarrativeVisualFragment fragment = new NarrativeVisualFragment(
                FragmentId.fromBlockId("BLK-001"),
                1,
                "SEG-001",
                "Apertura",
                "Texto narrado",
                new NarrativeVisualSlot(FragmentAssetRole.MAIN_IMAGE, "BND-MAIN", "IMG-MAIN", "assets/main.png",
                        FragmentAssetSource.NARRATIVE_LAYER, "", ""),
                new NarrativeVisualSlot(FragmentAssetRole.BRIDGE_TO_NEXT_FRAGMENT, "BND-BRIDGE", "IMG-BRIDGE", "assets/bridge.png",
                        FragmentAssetSource.NARRATIVE_LAYER, "", ""),
                "jobs/JOB-001/SEG-001.wav",
                3.5,
                true,
                true);

        SimpleVideoPlan plan = new BuildNarrativeVideoPlanUseCase().build(
                "Video narrativo",
                new NarrativeVideoWorkspaceProjection(List.of(fragment)));

        assertEquals(2, plan.frameCount());
        assertFalse(plan.frames().getFirst().silentVisual());
        assertEquals("assets/main.png", plan.frames().getFirst().imageRelativePath());
        assertEquals("jobs/JOB-001/SEG-001.wav", plan.frames().getFirst().audioRelativePath());
        assertTrue(plan.frames().get(1).silentVisual());
        assertEquals("SEG-001-BRIDGE", plan.frames().get(1).segmentId());
        assertEquals("assets/bridge.png", plan.frames().get(1).imageRelativePath());
        assertEquals(0, plan.framesMissingAudio());
        assertTrue(plan.exportableAsRenderedVideo());
    }
}
