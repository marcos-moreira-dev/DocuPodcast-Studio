package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** AUDIO-COMPRESS1: compressed exports must be planned without breaking WAV chunk recovery. */
final class AudioCompressionStrategyAc1SourceTest {
    @Test
    void strategyKeepsWavAsInternalCacheAndMovesCompressionToExportFirst() throws IOException {
        String doc = Files.readString(Path.of("docs/productizacion/AUDIO_COMPRESSION_STRATEGY_AC1.md"));
        assertTrue(doc.contains("conservar WAV por chunk"));
        assertTrue(doc.contains("MP3"));
        assertTrue(doc.contains("AAC"));
        assertTrue(doc.contains("AudioEncodingGateway"));
        assertTrue(doc.contains("FFmpeg"));
    }
}
