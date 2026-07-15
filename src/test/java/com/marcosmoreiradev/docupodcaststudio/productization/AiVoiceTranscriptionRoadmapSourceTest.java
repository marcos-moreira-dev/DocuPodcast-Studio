package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AiVoiceTranscriptionRoadmapSourceTest {
    @Test
    void aiVoiceRoadmapIsExplicitAndSttNoLongerBelongsToProductCore() throws Exception {
        String handoff = Files.readString(Path.of("AI_HANDOFF.md"));
        String readme = Files.readString(Path.of("README.md"));
        String scope = Files.readString(Path.of("docs/productizacion/T88C_LIMPIEZA_ALCANCE_MOTORES_GUI.md"));

        assertTrue(scope.contains("Coqui/XTTS"));
        assertTrue(scope.contains("Piper"));
        assertTrue(scope.contains("FFmpeg"));
        assertTrue(scope.contains("Whisper no pertenece al producto DocuPodcast"));
        assertTrue(handoff.contains("T88C"));
        assertTrue(readme.contains("Whisper no pertenece al núcleo de producto"));
        assertTrue(readme.contains("Se retira Audio a texto del flujo visible"));
    }

    @Test
    void handoffPointsFutureAgentsToCurrentScopeDocuments() throws Exception {
        String aiHandoff = Files.readString(Path.of("AI_HANDOFF.md"));
        assertTrue(aiHandoff.contains("T88C_LIMPIEZA_ALCANCE_MOTORES_GUI.md"));
        assertTrue(aiHandoff.contains("T89"));
        assertTrue(aiHandoff.contains("Release Candidate real"));
        assertFalse(aiHandoff.contains("STT como núcleo"));
    }
}
