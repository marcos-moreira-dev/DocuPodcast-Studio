package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImageEngineSmokeImageStoreTest {
    @Test
    void cleanupDeletesRegisteredTemporarySmokeImages() throws Exception {
        Path directory = ImageEngineSmokeImageStore.outputDirectory();
        Path png = directory.resolve("smoke-test.png");
        Files.createDirectories(directory);
        Files.write(png, new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 });

        assertTrue(Files.exists(png));

        ImageEngineSmokeImageStore.cleanupAll();

        assertFalse(Files.exists(png));
        assertFalse(Files.exists(directory));
    }
}
