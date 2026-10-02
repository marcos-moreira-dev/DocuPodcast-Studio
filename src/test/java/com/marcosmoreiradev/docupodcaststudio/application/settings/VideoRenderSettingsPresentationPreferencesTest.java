package com.marcosmoreiradev.docupodcaststudio.application.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class VideoRenderSettingsPresentationPreferencesTest {
    @Test
    void changesOnlyUserFacingRenderPreferences() {
        var original = new OperationalSettings.VideoRenderSettings("runtime/video.exe", "720P", false, 4.0,
                "https://runtime.invalid/archive.zip");

        var updated = original.withPresentationPreferences("4k", 8.5);

        assertEquals("4K", updated.resolutionPreset());
        assertEquals(8.5, updated.silentVisualBlockSeconds());
    }

    @Test
    void preservesAdapterCompatibilityValues() {
        var original = new OperationalSettings.VideoRenderSettings("runtime/video.exe", "1080P", false, 4.0,
                "https://runtime.invalid/archive.zip");

        var updated = original.withPresentationPreferences("2K", 5.0);

        assertEquals(original.ffmpegExecutable(), updated.ffmpegExecutable());
        assertEquals(original.ffmpegDownloadUrl(), updated.ffmpegDownloadUrl());
        assertFalse(updated.preferEmbeddedFfmpeg());
    }

    @Test
    void normalizesInvalidPresentationValuesThroughTheCanonicalSettingsContract() {
        var updated = OperationalSettings.VideoRenderSettings.defaults()
                .withPresentationPreferences("", -1.0);

        assertEquals("2K", updated.resolutionPreset());
        assertEquals(5.0, updated.silentVisualBlockSeconds());
    }

    @Test
    void changesManagedRuntimePreferenceWithoutLosingVideoValues() {
        var original = new OperationalSettings.VideoRenderSettings(
                "runtime/video.exe", "1080P", false, 7.0,
                "https://runtime.invalid/archive.zip");

        var updated = original.withRuntimePreference(true);

        assertEquals(original.ffmpegExecutable(), updated.ffmpegExecutable());
        assertEquals(original.resolutionPreset(), updated.resolutionPreset());
        assertEquals(original.silentVisualBlockSeconds(), updated.silentVisualBlockSeconds());
        assertEquals(original.ffmpegDownloadUrl(), updated.ffmpegDownloadUrl());
        org.junit.jupiter.api.Assertions.assertTrue(updated.preferEmbeddedFfmpeg());
    }
}
