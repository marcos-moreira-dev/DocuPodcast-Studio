package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class RuntimePathResolverTp1SourceTest {
    private static final Path MAIN = Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio");

    @Test
    void runtimeLayoutIsCentralizedForProductization() throws Exception {
        String resolver = Files.readString(MAIN.resolve("application/runtime/RuntimePathResolver.java"));
        String layout = Files.readString(MAIN.resolve("application/runtime/ApplicationRuntimeLayout.java"));
        String infra = Files.readString(MAIN.resolve("bootstrap/InfrastructureServicesFactory.java"));
        String settings = Files.readString(MAIN.resolve("presentation/settings/SettingsDialog.java"));
        assertTrue(resolver.contains("DOCUPODCAST_APP_ROOT"));
        assertTrue(resolver.contains("docupodcast.app.root"));
        assertTrue(layout.contains("tools/ffmpeg/bin/ffmpeg.exe"));
        assertTrue(layout.contains("tools/piper/piper.exe"));
        assertTrue(layout.contains("tools/xtts-wrapper"));
        assertTrue(infra.contains("RuntimePathResolver.defaultResolver().resolve()"));
        assertTrue(settings.contains("RuntimePathResolver.defaultResolver().resolve().applicationRoot()"));
    }
}
