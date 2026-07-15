package com.marcosmoreiradev.docupodcaststudio.presentation.script;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScriptWorkspaceRecordingPlaybackSourceTest {
    @Test
    void exposesVoiceAndPlaybackWithoutAudioToTextPromises() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/script/ScriptWorkspaceView.java"));

        assertTrue(source.contains("Usar voz IA"));
        assertTrue(source.contains("Preparar voz humana"));
        assertFalse(source.contains("Preparar audio a texto"));
        assertFalse(source.contains("prepareSpeechToTextForSelectedText"));
        assertTrue(source.contains("Reproducir desde selección"));
        assertTrue(source.contains("viewModel.selectScriptSegment"));
        assertTrue(source.contains("viewModel.playFromSelectedSegment")
                || source.contains("viewModel::playFromSelectedSegment"));
    }
}
