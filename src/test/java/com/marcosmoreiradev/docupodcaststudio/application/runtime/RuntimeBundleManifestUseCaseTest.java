package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RuntimeBundleManifestUseCaseTest {
    @Test
    void manifestDescribesToolsModelsScriptsAndExamples() throws Exception {
        Path root = Files.createTempDirectory("docupodcast-runtime-layout");
        Files.createDirectories(root.resolve("tools/ffmpeg/bin"));
        Files.createDirectories(root.resolve("tools/piper"));
        Files.createDirectories(root.resolve("tools/xtts-wrapper"));
        Files.createDirectories(root.resolve("models/tts/xtts"));
        Files.createDirectories(root.resolve("models/tts/piper/voices"));
        Files.createDirectories(root.resolve("scripts/tts"));
        Files.createDirectories(root.resolve("src/main/resources/examples"));

        RuntimeBundleManifest manifest = new BuildRuntimeBundleManifestUseCase()
                .execute(new ApplicationRuntimeLayout(root));

        Set<String> ids = manifest.items().stream().map(RuntimeBundleItem::id).collect(Collectors.toSet());
        assertTrue(ids.contains("ffmpeg-exe"));
        assertTrue(ids.contains("piper-exe"));
        assertTrue(ids.contains("xtts-wrapper"));
        assertTrue(ids.contains("tts-scripts"));
        assertTrue(ids.contains("examples-root"));
        assertFalse(manifest.toMarkdown().contains("PATH global"), "El contrato no debe depender de PATH global.");
    }
}
