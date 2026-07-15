package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.settings.ImageGenerationMemoryProfile;
import com.marcosmoreiradev.docupodcaststudio.application.settings.ImageGenerationSettings;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComfyUiLaunchArgumentPlannerTest {
    private final ComfyUiLaunchArgumentPlanner planner = new ComfyUiLaunchArgumentPlanner();

    @Test
    void parsesSupportedFlagsFromComfyHelpText() {
        ComfyUiRuntimeCapabilities capabilities = ComfyUiRuntimeCapabilities.fromHelpText("""
                usage: main.py [-h] [--listen LISTEN] [--port PORT] [--lowvram] [--normalvram] [--highvram] [--cpu-vae]
                """);

        assertTrue(capabilities.probed());
        assertTrue(capabilities.supports("--lowvram"));
        assertTrue(capabilities.supports("--normalvram"));
        assertTrue(capabilities.supports("--highvram"));
        assertTrue(capabilities.supports("--cpu-vae"));
        assertFalse(capabilities.supports("--does-not-exist"));
    }

    @Test
    void safeLowVramKeepsLegacyLowvramWhenRuntimeCouldNotBeProbed() {
        ImageGenerationSettings settings = settings(ImageGenerationMemoryProfile.SAFE_LOW_VRAM);

        assertEquals(List.of("--lowvram"),
                planner.memoryArguments(settings, ComfyUiRuntimeCapabilities.unknown("sin probe")));
    }

    @Test
    void normalProfileAddsNoMemoryFlags() {
        ImageGenerationSettings settings = settings(ImageGenerationMemoryProfile.NORMAL);
        ComfyUiRuntimeCapabilities capabilities = ComfyUiRuntimeCapabilities.fromHelpText("--lowvram --normalvram --highvram");

        assertEquals(List.of(), planner.memoryArguments(settings, capabilities));
    }

    @Test
    void vramRamOffloadUsesAggressiveCpuOffloadAndFp8WhenSupported() {
        ImageGenerationSettings settings = settings(ImageGenerationMemoryProfile.VRAM_RAM_OFFLOAD);

        assertEquals(List.of("--novram", "--cpu-vae", "--disable-smart-memory", "--fp8_e4m3fn-unet", "--fp8_e4m3fn-text-enc"),
                planner.memoryArguments(settings, ComfyUiRuntimeCapabilities.fromHelpText(
                        "--novram --cpu-vae --disable-smart-memory --fp8_e4m3fn-unet --fp8_e4m3fn-text-enc")));
        assertEquals(List.of("--novram"),
                planner.memoryArguments(settings, ComfyUiRuntimeCapabilities.fromHelpText("--novram")));
        assertEquals(List.of("--novram"),
                planner.memoryArguments(settings, ComfyUiRuntimeCapabilities.unknown("sin probe")));
    }

    @Test
    void highMemoryProfileUsesOnlyFlagsSupportedByRuntime() {
        ImageGenerationSettings settings = settings(ImageGenerationMemoryProfile.HIGH_MEMORY);

        assertEquals(List.of("--highvram"),
                planner.memoryArguments(settings, ComfyUiRuntimeCapabilities.fromHelpText("--normalvram --highvram")));
        assertEquals(List.of("--highvram"),
                planner.memoryArguments(settings, ComfyUiRuntimeCapabilities.fromHelpText("--highvram")));
        assertEquals(List.of(),
                planner.memoryArguments(settings, ComfyUiRuntimeCapabilities.unknown("sin probe")));
    }

    @Test
    void legacyLowVramBooleanMigratesIntoExplicitMemoryProfile() {
        ImageGenerationSettings safe = new ImageGenerationSettings(
                "managed-local", "http://127.0.0.1:8188", "AUTO", "TEST_4GB_SD15",
                "model.safetensors", "models/image/adapters", 300, true);
        ImageGenerationSettings normal = new ImageGenerationSettings(
                "managed-local", "http://127.0.0.1:8188", "AUTO", "TEST_4GB_SD15",
                "model.safetensors", "models/image/adapters", 300, false);
        ImageGenerationSettings high = settings(ImageGenerationMemoryProfile.HIGH_MEMORY);
        ImageGenerationSettings offload = settings(ImageGenerationMemoryProfile.VRAM_RAM_OFFLOAD);

        assertEquals("SAFE_LOW_VRAM", safe.memoryProfile());
        assertTrue(safe.lowVram());
        assertEquals("NORMAL", normal.memoryProfile());
        assertFalse(normal.lowVram());
        assertEquals("HIGH_MEMORY", high.memoryProfile());
        assertFalse(high.lowVram());
        assertEquals("VRAM_RAM_OFFLOAD", offload.memoryProfile());
        assertFalse(offload.lowVram());
    }

    private static ImageGenerationSettings settings(ImageGenerationMemoryProfile profile) {
        return new ImageGenerationSettings(
                "managed-local",
                "http://127.0.0.1:8188",
                "AUTO",
                "TEST_4GB_SD15",
                "model.safetensors",
                "models/image/adapters",
                300,
                profile.legacyLowVram(),
                profile.name(),
                2);
    }
}
