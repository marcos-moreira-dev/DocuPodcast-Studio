package com.marcosmoreiradev.docupodcaststudio.infrastructure.recording;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class JavaSoundAudioRecordingGatewaySourceTest {
    @Test
    void recordsProjectLocalWavWithJavaSoundAndSafeName() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/recording/JavaSoundAudioRecordingGateway.java"));

        assertTrue(source.contains("TargetDataLine"));
        assertTrue(source.contains("AudioSystem.write"));
        assertTrue(source.contains("16_000.0f"));
        assertTrue(source.contains("RECORDINGS_DIR"));
        assertTrue(source.contains("safeWavFileName"));
        assertTrue(source.contains("docupodcast-voice-recording"));
    }
}
