package com.marcosmoreiradev.docupodcaststudio.localmedia;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ComfyUiMemoryProfileTest {
    @Test void mapsEveryPersistedProfileToTheEffectiveComfyUiFlag() {
        assertEquals(List.of("--lowvram"), ComfyUiMemoryProfile.from("SAFE_LOW_VRAM").launchArguments());
        assertEquals(List.of(), ComfyUiMemoryProfile.from("NORMAL").launchArguments());
        assertEquals(List.of("--novram"), ComfyUiMemoryProfile.from("VRAM_RAM_OFFLOAD").launchArguments());
        assertEquals(List.of("--highvram"), ComfyUiMemoryProfile.from("HIGH_MEMORY").launchArguments());
    }

    @Test void unknownProfileFallsBackToTheSafeFourGigabytePolicy() {
        assertEquals(ComfyUiMemoryProfile.SAFE_LOW_VRAM, ComfyUiMemoryProfile.from("unknown"));
    }
}
