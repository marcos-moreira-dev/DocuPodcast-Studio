package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceLibraryDeconstructionT121V01SourceTest {
    @Test
    void voiceLibraryNoLongerOwnsDocumentAssignmentOrLegacyRoleLanguage() throws Exception {
        String view = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java"));
        String engineControls = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceEngineSettingsControls.java"));
        String overview = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceManageOverviewPanel.java"));
        String source = view + "\n" + engineControls + "\n" + overview;

        assertTrue(source.contains("Consulta qué voces puede usar Documento"));
        assertTrue(source.contains("Documento usa voces listas"));
        assertTrue(source.contains("Voz IA avanzada"));
        assertTrue(source.contains("Voz local simple"));
        assertTrue(engineControls.contains("Modo de prueba"));
        assertFalse(source.contains("Asignar al segmento seleccionado"));
        assertFalse(source.contains("assignVoiceToSelectedSegment"));
        assertFalse(source.contains("CharacterProfile"));
        assertFalse(source.contains("PerformanceStyle"));
        assertFalse(source.contains("Personajes / roles"));
        assertFalse(source.contains("Estilos de interpretación"));
        assertFalse(source.contains("Coqui"));
        assertFalse(source.contains("XTTS"));
    }

    @Test
    void voiceLibraryStopsUsingLegacyScriptCssClasses() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java"));

        assertFalse(source.contains("script-title"));
        assertFalse(source.contains("script-summary"));
        assertFalse(source.contains("script-segment-text"));
        assertFalse(source.contains("script-segment-header"));
        assertFalse(source.contains("script-segment-chips"));
        assertTrue(source.contains("voice-library-title"));
        assertTrue(source.contains("voice-library-summary"));
        assertTrue(source.contains("voice-library-body"));
    }
}
