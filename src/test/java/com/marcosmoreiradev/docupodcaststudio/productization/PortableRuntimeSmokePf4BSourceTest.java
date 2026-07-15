package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** PF4B guards the post-app-image smoke path for portable double-click execution. */
final class PortableRuntimeSmokePf4BSourceTest {
    @Test
    void portableRuntimeSmokeScriptChecksLauncherAndRuntimeRoot() throws Exception {
        String script = Files.readString(Path.of("scripts/34-smoke-app-portable-runtime.bat"));
        assertTrue(script.contains("PF4B_PORTABLE_RUNTIME_SMOKE_REPORT.md"));
        assertTrue(script.contains("DOCUPODCAST_APP_ROOT=%%~dp0"));
        assertTrue(script.contains("run-docupodcast-studio.bat"));
        assertTrue(script.contains("dist\\portable\\%APP_NAME%"));
        assertTrue(script.contains("Runtime root esperado al ejecutar portable"));
    }

    @Test
    void releaseCandidateRunsPortableRuntimeSmoke() throws Exception {
        String rc = Files.readString(Path.of("scripts/16-release-candidate.bat"));
        assertTrue(rc.contains("34-smoke-app-portable-runtime.bat"));
        assertTrue(rc.contains("PF4B portable smoke"));
    }
}
