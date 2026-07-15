package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceLibraryWorkspaceSourceTest {
    @Test
    void workspaceIsLibraryOnlyAndNoLongerAssignsFragments() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java"));

        assertTrue(source.contains("Biblioteca de voces"));
        assertTrue(source.contains("Voces disponibles"));
        assertTrue(source.contains("Muestras de voz"));
        assertTrue(source.contains("Configurar motor"));
        assertTrue(source.contains("Consulta qué voces puede usar Documento"));
        assertTrue(source.contains("Documento usa voces listas"));
        assertFalse(source.contains("Asignar al segmento seleccionado"));
        assertFalse(source.contains("Uso desde Documento"));
        assertFalse(source.contains("Personajes / roles"));
        assertFalse(source.contains("Estilos de interpretación"));
    }
}
