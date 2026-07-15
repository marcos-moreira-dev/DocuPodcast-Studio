package com.marcosmoreiradev.docupodcaststudio.application.narrative;

import com.marcosmoreiradev.docupodcaststudio.application.fragment.FragmentWorkspaceProjection;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.DocumentFragment;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetRole;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetSource;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentId;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildNarrativeVideoWorkspaceProjectionUseCaseTest {
    @Test
    void associatesMainImageBridgeImageAndAudioToTheSameFragment() {
        FragmentId fragmentId = FragmentId.fromBlockId("BLK-001");
        DocumentFragment fragment = new DocumentFragment(
                fragmentId,
                1,
                "Texto narrado",
                "txt",
                null,
                "BLK-001",
                "SEG-001",
                "",
                FragmentStatus.AUDIO_READY,
                Map.of("title", "Apertura"));
        FragmentWorkspaceProjection source = new FragmentWorkspaceProjection(
                List.of(fragment),
                List.of(
                        binding("BND-STORY", fragmentId, "IMG-STORY", FragmentAssetRole.MAIN_IMAGE, FragmentAssetSource.STORYBOARD, "assets/story.png"),
                        binding("BND-MAIN", fragmentId, "IMG-MAIN", FragmentAssetRole.MAIN_IMAGE, FragmentAssetSource.NARRATIVE_LAYER, "assets/main.png"),
                        binding("BND-BRIDGE", fragmentId, "IMG-BRIDGE", FragmentAssetRole.BRIDGE_TO_NEXT_FRAGMENT, FragmentAssetSource.NARRATIVE_LAYER, "assets/bridge.png"),
                        binding("BND-AUDIO", fragmentId, "AUD-001", FragmentAssetRole.AUDIO_TTS, FragmentAssetSource.AUDIO_JOB, "jobs/JOB-001/SEG-001.wav")
                ));

        NarrativeVideoWorkspaceProjection projection = new BuildNarrativeVideoWorkspaceProjectionUseCase().build(source);
        NarrativeVisualFragment visual = projection.fragments().getFirst();

        assertEquals("IMG-MAIN", visual.mainImage().assetId());
        assertEquals("assets/main.png", visual.mainImage().assetPath());
        assertEquals("IMG-BRIDGE", visual.bridgeImage().assetId());
        assertEquals("jobs/JOB-001/SEG-001.wav", visual.audioRelativePath());
        assertTrue(visual.exportReady());
        assertEquals(1, projection.bridgeImageCount());
    }

    private static FragmentAssetBinding binding(
            String id,
            FragmentId fragmentId,
            String assetId,
            FragmentAssetRole role,
            FragmentAssetSource source,
            String path
    ) {
        return new FragmentAssetBinding(id, fragmentId, assetId, role, source, path, "READY", assetId, Map.of("notes", "prompt"));
    }
}
