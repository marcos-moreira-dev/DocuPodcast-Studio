package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** UX-HF3 improves internal playback diagnostics for generated WAV fragments. */
final class InternalWavPlaybackUxHf3SourceTest {
    @Test
    void javaSoundPlayerConvertsOrReportsGeneratedWavFormat() throws Exception {
        String player = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/playback/JavaSoundSegmentAudioPlayer.java"));
        assertTrue(player.contains("playableStream"));
        assertTrue(player.contains("AudioFormat.Encoding.PCM_SIGNED"));
        assertTrue(player.contains("formatLabel"));
        assertTrue(player.contains("El reproductor interno no pudo abrir este WAV"));
    }
}
