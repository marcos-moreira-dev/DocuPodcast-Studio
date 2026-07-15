package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TechnicalProblemT122SourceTest {
    @Test
    void dialogCapturesFastInkSamplesAndCommitsEditableStrokesOnRelease() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java");
        String engine = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/InkRealtimeStrokeEngine.java");

        assertTrue(engine.contains("Boolean.getBoolean(\"docupodcast.ink.perfDiagnostics\")"));
        assertTrue(engine.contains("Boolean.getBoolean(\"docupodcast.ink.fastDebug\")"));
        assertTrue(engine.contains("FRAME_BUDGET_NANOS"));
        assertTrue(engine.contains("Deque<RawPoint>"));
        assertTrue(engine.contains("AnimationTimer"));
        assertTrue(engine.contains("previewQuadratic"));
        assertTrue(dialog.contains("inkEngine.begin"));
        assertTrue(dialog.contains("inkEngine.move"));
        assertTrue(dialog.contains("inkEngine.end"));
        assertTrue(dialog.contains("inkEngine.flushAll"));
        assertTrue(dialog.contains("drawingSurface.beginLiveStroke()"));
        assertTrue(dialog.contains("drawingSurface.previewLine"));
        assertTrue(dialog.contains("drawingSurface.commitInkStroke"));
        assertTrue(dialog.contains("Deque<CanvasUndoSnapshot>"));
        assertTrue(dialog.contains("rememberFastInkUndo()"));
        assertTrue(dialog.contains("CanvasUndoSnapshot.vector"));
        assertTrue(dialog.contains("drawingSurface.restoreInkUndoState"));
        assertFalse(dialog.contains("maybeGrowCanvasForPoint(point.getX(), point.getY())"));
        assertFalse(dialog.contains("canvasStateJson();\n        draw"));
        assertFalse(dialog.contains("queueStrokeSample"));
        assertFalse(dialog.contains("flushPendingStrokeSamples"));
        assertFalse(dialog.contains("pendingStrokeSamples"));
        assertFalse(dialog.contains("commitBufferedStroke()"));
        assertFalse(dialog.contains("InkStrokePipeline.resampleLine"));
    }

    @Test
    void canvasSurfaceKeepsLiveLayerAndEditableRawStrokeModel() throws Exception {
        String surface = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurface.java");

        assertTrue(surface.contains("liveStrokeLayer"));
        assertTrue(surface.contains("liveStrokeTiles"));
        assertTrue(surface.contains("record InkPointState"));
        assertTrue(surface.contains("record InkStrokeState"));
        assertTrue(surface.contains("List<InkStrokeState> inkStrokeStates()"));
        assertTrue(surface.contains("restoreInkStrokeStates"));
        assertTrue(surface.contains("vectorInkReliable()"));
        assertTrue(surface.contains("restoreInkUndoState"));
        assertTrue(surface.contains("commandsForStroke"));
        assertTrue(surface.contains("quadraticCurveTo"));
        assertTrue(surface.contains("graphics.clearRect("));
        String newStrokePath = surface.substring(surface.indexOf("private List<InkCommand> commandsForStroke"),
                surface.indexOf("private static double widthForPoint"));
        assertFalse(newStrokePath.contains("InkCommand.quadratic"));
        assertTrue(newStrokePath.contains("InkCommand.line"));
    }

    @Test
    void sidecarWritesGenericInkWorkspaceStateAndKeepsLegacyCommandFallback() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java");
        String state = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/ink/InkWorkspaceState.java");
        String serializer = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/ink/InkWorkspaceStateSerializer.java");

        assertTrue(state.contains("CURRENT_VERSION = 3"));
        assertTrue(serializer.contains("\\\"version\\\":"));
        assertTrue(dialog.contains("InkWorkspaceStateSerializer.toJson"));
        assertTrue(serializer.contains("\\\"inkStrokes\\\""));
        assertTrue(serializer.contains("\\\"points\\\""));
        assertTrue(serializer.contains("\\\"nanos\\\""));
        assertTrue(dialog.contains("drawingSurface.restoreInkStrokeStates(editableStrokes)"));
        assertTrue(dialog.contains("drawingSurface.restoreInkCommandStates(strokes)"));
        assertTrue(dialog.contains("\"strokes\""));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
