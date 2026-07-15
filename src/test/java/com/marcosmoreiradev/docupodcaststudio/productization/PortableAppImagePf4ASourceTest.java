package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PF4A guards the portable app-image path for a normal double-click user. */
final class PortableAppImagePf4ASourceTest {
    @Test
    void appImageCreatesPortableLauncherWithAppRoot() throws Exception {
        String script = Files.readString(Path.of("scripts/14-app-image-completa.bat"));
        assertTrue(script.contains("run-docupodcast-studio.bat"));
        assertTrue(script.contains("DOCUPODCAST_APP_ROOT=%%~dp0"));
        assertTrue(script.contains("%APP_NAME%.exe"));
        assertTrue(script.contains("define DOCUPODCAST_APP_ROOT para doble clic limpio"));
        assertFalse(script.contains("mock integrado salvo DOCUPODCAST_TTS_COMMAND"));
    }

    @Test
    void portableLayoutCarriesLauncherAndRuntimeManifest() throws Exception {
        String script = Files.readString(Path.of("scripts/32-preparar-app-portable-layout.bat"));
        assertTrue(script.contains("run-docupodcast-studio.bat"));
        assertTrue(script.contains("DOCUPODCAST_APP_ROOT=%%~dp0"));
        assertTrue(script.contains("Runtime root esperado al ejecutar portable"));
        assertTrue(script.contains("PF4A Portable Manifest"));
    }
}
