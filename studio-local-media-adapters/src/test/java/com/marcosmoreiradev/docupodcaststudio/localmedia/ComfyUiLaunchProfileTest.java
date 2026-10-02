package com.marcosmoreiradev.docupodcaststudio.localmedia;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComfyUiLaunchProfileTest {
    @Test
    void combinesResolvedComputeAndMemoryArgumentsAndDelegatesLiveVerification() {
        ComfyUiLaunchProfile profile = ComfyUiLaunchProfile.resolved(
                "gpu-nvidia-0",
                "NVIDIA GeForce GTX 1650",
                "CUDA",
                true,
                List.of("--cuda-device", "0"),
                List.of("--novram", "--cpu-vae"),
                json -> json.contains("\"cuda\"") ? "" : "backend incorrecto");

        assertEquals(List.of("--cuda-device", "0", "--novram", "--cpu-vae"),
                profile.launchArguments());
        assertTrue(profile.gpu());
        assertEquals("", profile.verificationFailure("{\"type\":\"cuda\"}"));
        assertEquals("backend incorrecto", profile.verificationFailure("{\"type\":\"cpu\"}"));
    }
}
