package com.marcosmoreiradev.docupodcaststudio.application.video;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VideoRenderProgressTest {
    @Test
    void renderProgressBlocksMainWorkspaceAndCalculatesRatio() {
        VideoRenderProgress progress = VideoRenderProgress.preparing(20);
        assertEquals(VideoRenderStage.PREPARING, progress.stage());
        assertTrue(progress.mainWorkspaceBlocked());
        assertTrue(progress.cancellable());
        assertEquals(0.0, progress.ratio());
        assertEquals(0.5, new VideoRenderProgress(VideoRenderStage.RENDERING_WITH_FFMPEG, 10, 20,
                "Renderizando frame", true, true).ratio());
    }

    @Test
    void finalAssemblyIsExplicitAndRemainsCancellable() {
        VideoRenderProgress progress = VideoRenderProgress.assembling(1645, "Uniendo clips.");

        assertEquals(VideoRenderStage.ASSEMBLING_FINAL, progress.stage());
        assertEquals(0, progress.completedFrames());
        assertEquals(0.0, progress.ratio());
        assertTrue(progress.determinateProgress());
        assertTrue(progress.cancellable());
        assertEquals("Uniendo clips.", progress.currentStep());
    }
}
