package com.marcosmoreiradev.docupodcaststudio.presentation.script;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScriptWorkspaceSourceTest {
    @Test
    void scriptWorkspaceIsStructuredAndDoesNotUseCanvas() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/script/ScriptWorkspaceView.java"));

        assertTrue(source.contains("Narración avanzada"));
        assertTrue(source.contains("Crear desde documento"));
        assertFalse(source.contains("InteractiveCanvas"));
        assertFalse(source.contains("ZoomableDiagramSurface"));
    }

    @Test
    void scriptWorkspaceRendersNarrativeEditorReadinessCardsAndSegmentStatusChips() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/script/ScriptWorkspaceView.java"));
        String css = Files.readString(Path.of("src/main/resources/css/script-workspace.css"));
        String projection = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/script/ScriptSegmentPresentation.java"));

        assertTrue(source.contains("script-editor-header"));
        assertTrue(source.contains("script-readiness-card"));
        assertTrue(source.contains("script-status-chip"));
        assertTrue(source.contains("Playback"));
        assertTrue(source.contains("playbackSyncState"));
        assertTrue(source.contains("selectedSegmentStatusLabel"));
        assertTrue(source.contains("Estado: "));
        assertTrue(source.contains("validationIssueList"));
        assertTrue(source.contains("presentationFor(segment, issues)"));
        assertTrue(projection.contains("voiceReady"));
        assertTrue(projection.contains("audioReady"));
        assertTrue(projection.contains("storyboardReady"));
        assertTrue(projection.contains("playbackStatus"));
        assertTrue(css.contains(".script-readiness-card"));
        assertTrue(css.contains(".script-status-chip"));
        assertTrue(css.contains(".script-segment-paused"));
        assertTrue(css.contains(".script-status-playing"));
    }
}
