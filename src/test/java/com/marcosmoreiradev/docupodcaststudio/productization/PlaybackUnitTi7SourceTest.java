package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlaybackUnitTi7SourceTest {
    @Test
    void playbackManifestIsUnitAwareAndDoesNotExposeVisualSilentAsAudioCue() throws IOException {
        String playback = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/playback/BuildPlaybackManifestUseCase.java");
        assertTrue(playback.contains("RenderUnitPlan"));
        assertTrue(playback.contains("renderUnitPlan.audioUnits()"));
        assertTrue(playback.contains("unit.usesExternalAudio()"));
        assertTrue(playback.contains("unit.requiresAudioGeneration()"));
        assertTrue(playback.contains("cueForUnit"));

        String seek = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/playback/SeekPlaybackUseCase.java");
        assertTrue(seek.contains("seekUnit"));
        assertTrue(seek.contains("manifest.cueForUnit(unitId)"));

        String docs = read("docs/productizacion/TI7_PLAYBACK_ORACION_UNIDAD.md");
        assertTrue(docs.contains("Playback por oración/unidad"));
        assertTrue(docs.contains("VISUAL_SILENT no se reproduce como audio"));
        assertTrue(docs.contains("tablas, imágenes y fórmulas no narrables"));
    }

    private static String read(String path) throws IOException {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
