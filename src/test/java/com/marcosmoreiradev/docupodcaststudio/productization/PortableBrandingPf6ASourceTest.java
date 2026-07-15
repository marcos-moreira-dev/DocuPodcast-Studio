package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** PF6A ensures portable/app-image branding survives packaging and smoke validation. */
final class PortableBrandingPf6ASourceTest {
    @Test
    void portableLayoutCopiesBrandingAssets() throws Exception {
        String portable = Files.readString(Path.of("scripts/32-preparar-app-portable-layout.bat"));
        assertTrue(portable.contains("packaging\\windows"));
        assertTrue(portable.contains("%PORTABLE_DIR%\\branding"));
        assertTrue(portable.contains("docupodcast-icon.ico"));
        assertTrue(portable.contains("docupodcast-icon.png"));
    }

    @Test
    void smokeAndReleaseCandidateMentionPortableBranding() throws Exception {
        String smoke = Files.readString(Path.of("scripts/34-smoke-app-portable-runtime.bat"));
        String rc = Files.readString(Path.of("scripts/16-release-candidate.bat"));
        assertTrue(smoke.contains("branding\\docupodcast-icon.ico"));
        assertTrue(smoke.contains("branding\\docupodcast-icon.png"));
        assertTrue(rc.contains("PF5B/PF6A branding"));
    }
}
