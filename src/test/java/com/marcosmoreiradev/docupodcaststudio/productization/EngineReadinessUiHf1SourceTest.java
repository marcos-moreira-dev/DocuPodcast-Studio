package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class EngineReadinessUiHf1SourceTest {
    @Test
    void engineReadinessUsesHumanOperationalStatesAndDocumentHidesBrokenAdvancedVoice() throws Exception {
        String item = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/audio/AudioEngineReadinessUiItem.java"));
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/audio/InspectAudioEngineReadinessUiUseCase.java"));
        String documentPanel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java"));
        String voiceWorkspace = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java"));

        assertTrue(item.contains("No aparece en Documento: falta prueba WAV válida"));
        assertTrue(item.contains("compactLine()"));
        assertTrue(useCase.contains("ListAudioEngineAvailabilityUseCase"));
        assertTrue(documentPanel.contains("audioEngineReadinessLines()"));
        assertTrue(documentPanel.contains("Voz IA avanzada no aparece aquí si no pasó prueba WAV"));
        assertTrue(voiceWorkspace.contains("report.readinessLabel()"));
    }
}
