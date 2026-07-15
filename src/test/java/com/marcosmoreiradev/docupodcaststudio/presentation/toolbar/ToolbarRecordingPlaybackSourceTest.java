package com.marcosmoreiradev.docupodcaststudio.presentation.toolbar;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ToolbarRecordingPlaybackSourceTest {
    @Test
    void contextualToolbarExposesVoiceAndPlaybackButNoAudioToTextProductPromise() throws Exception {
        String provider = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/WorkspaceToolbarActionProvider.java"));
        String toolbar = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/MainToolbarView.java"));

        assertTrue(provider.contains("Grabar voz"));
        assertTrue(provider.contains("OPEN_VOICE_LIBRARY"));
        assertFalse(provider.contains("Audio a texto"));
        assertTrue(toolbar.contains("shellView.dispatchCommand(action.commandId())"));
        assertTrue(toolbar.contains("case OPEN_VOICE_LIBRARY, PREPARE_AI_VOICE, PREPARE_HUMAN_VOICE, IMPORT_VOICE_SAMPLE"));
        assertFalse(toolbar.contains("handleTranscribeAudioToText"));
        assertFalse(toolbar.contains("Audio a texto"));
    }
}
