package com.marcosmoreiradev.docupodcaststudio.presentation.ink;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InkRealtimeStrokeEngineSourceTest {
    @Test
    void engineKeepsDragHotPathBufferedAndFrameBudgeted() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/InkRealtimeStrokeEngine.java"));

        assertTrue(source.contains("FRAME_BUDGET_NANOS = 8_000_000L"));
        assertTrue(source.contains("MAX_POINTS_PER_FRAME = 512"));
        assertTrue(source.contains("pendingPoints.addLast(RawPoint.move"));
        assertTrue(source.contains("Deque<RawPoint>"));
        assertTrue(source.contains("AnimationTimer"));
        assertTrue(source.contains("sink.previewLine(lastX, lastY, x, y, color, width, erase)"));
        assertTrue(source.contains("previewQuadratic"));
        assertTrue(source.contains("previewStep("));
        assertTrue(source.contains("commitStroke(new CommittedStroke"));
        assertTrue(source.contains("Boolean.getBoolean(\"docupodcast.ink.perfDiagnostics\")"));
        assertTrue(source.contains("Boolean.getBoolean(\"docupodcast.ink.inputDiagnostics\")"));
        assertTrue(source.contains("Boolean.getBoolean(\"docupodcast.ink.renderDiagnostics\")"));
        assertTrue(source.contains("pointsPerSecond="));
        assertFalse(source.contains("LIVE_RESAMPLE_SPACING"));
        assertFalse(source.contains("InkStrokePipeline"));
        assertFalse(source.contains("snapshot("));
    }
}
