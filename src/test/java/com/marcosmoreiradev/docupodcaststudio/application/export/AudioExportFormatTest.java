package com.marcosmoreiradev.docupodcaststudio.application.export;

import com.marcosmoreiradev.docupodcaststudio.domain.export.AudioExportFormat;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AudioExportFormatTest {
    @Test
    void detectsFinalAudioFormatFromTargetExtension() {
        assertEquals(AudioExportFormat.WAV, AudioExportFormat.fromTarget(Path.of("podcast.wav")));
        assertEquals(AudioExportFormat.MP3, AudioExportFormat.fromTarget(Path.of("podcast.mp3")));
        assertEquals(AudioExportFormat.AAC, AudioExportFormat.fromTarget(Path.of("podcast.aac")));
        assertEquals(AudioExportFormat.WAV, AudioExportFormat.fromTarget(Path.of("podcast")));
    }

    @Test
    void normalizesTargetExtensionForCompressedAudio() {
        assertEquals(Path.of("podcast.mp3"), AudioExportFormat.MP3.normalizeTarget(Path.of("podcast")));
        assertEquals(Path.of("podcast.aac"), AudioExportFormat.AAC.normalizeTarget(Path.of("podcast")));
    }
}
