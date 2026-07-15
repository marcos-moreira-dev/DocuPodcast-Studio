package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceLibraryCapabilityUiSourceTest {
    @Test
    void workspaceUsesCapabilityReportAndHonestStatusChips() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java"));
        String css = Files.readString(Path.of("src/main/resources/css/voice-library.css"));

        assertTrue(source.contains("VoiceLibraryCapabilityReport"));
        assertTrue(source.contains("viewModel.voiceCapabilityReport()"));
        assertTrue(source.contains("Prueba rápida del motor seleccionado"));
        assertTrue(source.contains("report.readinessLabel()"));
        assertTrue(source.contains("voice-capability-chip"));
        assertTrue(css.contains("voice-capability-ready"));
        assertTrue(css.contains("voice-capability-roadmap"));
        assertTrue(css.contains("voice-capability-blocked"));
    }
}
