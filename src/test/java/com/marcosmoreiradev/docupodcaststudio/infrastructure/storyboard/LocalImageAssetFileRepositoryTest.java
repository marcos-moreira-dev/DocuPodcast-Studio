package com.marcosmoreiradev.docupodcaststudio.infrastructure.storyboard;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LocalImageAssetFileRepositoryTest {
    @TempDir
    Path temp;

    @Test
    void rejectsGenerationMetadataAsImage() throws Exception {
        Path metadata = Files.writeString(temp.resolve("generation.properties"), "status=accepted");
        org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException.class,
                () -> new LocalImageAssetFileRepository().importImage(temp.resolve("project.json"),
                        metadata, "IMG-001", "metadata", "test", ""));
        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(temp.resolve("media")));
    }

    @Test
    void importsImageAsRelativeProjectAsset() throws Exception {
        Path source = temp.resolve("foto.png");
        Files.write(source, new byte[] {1, 2, 3, 4});
        Path projectFile = temp.resolve("Proyecto.docupodcast.json");

        var asset = new LocalImageAssetFileRepository().importImage(projectFile, source, "IMG-001", "Foto", "test", "");

        assertEquals(ProjectAssetKind.IMAGE, asset.kind());
        assertEquals("media/images/img-001-foto.png", asset.relativePath());
        assertTrue(Files.exists(temp.resolve(asset.relativePath())));
        assertTrue(asset.checksum().startsWith("sha256:"));
    }
}
