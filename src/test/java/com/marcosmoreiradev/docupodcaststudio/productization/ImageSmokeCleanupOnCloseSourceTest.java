package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImageSmokeCleanupOnCloseSourceTest {
    @Test
    void appCloseCleansTemporaryImageSmokeStoreOnlyAfterCloseIsAccepted() throws Exception {
        String bootstrap = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationBootstrap.java"));

        assertTrue(bootstrap.contains("ImageEngineSmokeImageStore.cleanupAll()"));
        assertTrue(bootstrap.contains("if (!event.isConsumed())"));
    }
}
