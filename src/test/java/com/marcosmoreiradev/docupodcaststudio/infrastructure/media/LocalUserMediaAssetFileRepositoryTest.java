package com.marcosmoreiradev.docupodcaststudio.infrastructure.media;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LocalUserMediaAssetFileRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void importsAudioInsideProjectMediaAudioFolder() throws Exception {
        Path projectFile = tempDir.resolve("Demo").resolve("Demo.docupodcast.json");
        Files.createDirectories(projectFile.getParent());
        Path source = tempDir.resolve("Lucia hablando.mp3");
        Files.writeString(source, "audio");
        LocalUserMediaAssetFileRepository repository = new LocalUserMediaAssetFileRepository();

        ProjectAssetReference asset = repository.importAudioClip(projectFile, source, "AUDIO-USER-001", "Lucía hablando", "voz", "");

        assertEquals(ProjectAssetKind.AUDIO_CLIP, asset.kind());
        assertTrue(asset.relativePath().startsWith("media/audio/"));
        assertTrue(Files.isRegularFile(projectFile.getParent().resolve(asset.relativePath())));
        assertTrue(asset.checksum().startsWith("sha256:"));
    }

    @Test
    void importsVideoOriginalInsideProjectMediaVideoFolder() throws Exception {
        Path projectFile = tempDir.resolve("Demo").resolve("Demo.docupodcast.json");
        Files.createDirectories(projectFile.getParent());
        Path source = tempDir.resolve("teatro.mp4");
        Files.writeString(source, "video");
        LocalUserMediaAssetFileRepository repository = new LocalUserMediaAssetFileRepository();

        ProjectAssetReference asset = repository.importVideoSource(projectFile, source, "VIDEO-SOURCE-001", "teatro.mp4", "original", "");

        assertEquals(ProjectAssetKind.VIDEO_SOURCE, asset.kind());
        assertTrue(asset.relativePath().startsWith("media/video/"));
        assertTrue(asset.isVideo());
    }
}
