package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ensures managed voice engines do not reuse stale raw commands from previous ZIP/tanda folders. */
final class AdvancedVoiceManagedRuntimeHf2SourceTest {
    @Test
    void managedEnginesDeriveCommandsFromCurrentRuntimeRoot() throws Exception {
        String advanced = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/XttsTtsCommandTemplate.java"));
        String simple = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/PiperTtsCommandTemplate.java"));
        String aware = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/SettingsAwareAudioGenerationGateway.java"));

        assertFalse(advanced.contains("Explicit commands always win"));
        assertFalse(simple.contains("Explicit command templates always win"));
        assertTrue(advanced.contains("current application root"));
        assertTrue(simple.contains("current application folder"));
        assertTrue(aware.contains("effectiveTtsDisplayName(settings, command)"));
        assertTrue(aware.contains("effectiveTtsComputePolicy(settings, command)"));
    }
}
