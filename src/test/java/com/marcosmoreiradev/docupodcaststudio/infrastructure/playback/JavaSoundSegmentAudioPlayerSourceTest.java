package com.marcosmoreiradev.docupodcaststudio.infrastructure.playback;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class JavaSoundSegmentAudioPlayerSourceTest {
    @Test
    void playerUsesStandardJavaSoundAndStaysWavFirst() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/playback/JavaSoundSegmentAudioPlayer.java"));

        assertTrue(source.contains("javax.sound.sampled"));
        assertTrue(source.contains("AudioSystem.getAudioInputStream"));
        assertTrue(source.contains("Clip"));
        assertTrue(source.contains("currentPositionSeconds"));
        assertTrue(source.contains("setFadeEnvelope"));
        assertTrue(source.contains("fadeGain"));
    }
}
