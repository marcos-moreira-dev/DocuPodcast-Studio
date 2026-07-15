package com.marcosmoreiradev.docupodcaststudio.application.video;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EmbeddedFfmpegLocatorTest {
    @Test
    void prefersEmbeddedToolsFolderBeforeExternalConfiguration() throws Exception {
        Path root = Files.createTempDirectory("docupodcast-ffmpeg");
        Path ffmpeg = root.resolve(EmbeddedFfmpegLocator.EMBEDDED_FFMPEG_RELATIVE_PATH);
        Files.createDirectories(ffmpeg.getParent());
        Files.writeString(ffmpeg, "fake");
        FfmpegToolDiscovery discovery = new EmbeddedFfmpegLocator().locate(root, null);
        assertTrue(discovery.ready());
        assertTrue(discovery.embedded());
        assertTrue(discovery.ffmpegExecutable().endsWith(Path.of("tools", "ffmpeg", "bin", "ffmpeg.exe")));
    }

    @Test
    void reportsMissingEmbeddedFfmpegWithoutTouchingPath() throws Exception {
        Path root = Files.createTempDirectory("docupodcast-ffmpeg-missing");
        FfmpegToolDiscovery discovery = new EmbeddedFfmpegLocator().locate(root, null);
        assertFalse(discovery.ready());
        assertTrue(discovery.message().contains("FFmpeg embebido"));
    }
}
