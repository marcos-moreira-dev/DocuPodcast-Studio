package com.marcosmoreiradev.docupodcaststudio.application.compute;

import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoResolutionPreset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProductionHardwareProfilePolicyTest {
    private final ProductionHardwareProfilePolicy policy = new ProductionHardwareProfilePolicy();

    @Test
    void fourGbVramUsesConservativeProfile() {
        ProductionHardwareProfileAdvice advice = policy.advise(4096, SimpleVideoResolutionPreset.UHD_4K, 4);

        assertTrue(advice.conservative());
        assertFalse(advice.highCapacity());
        assertEquals(SimpleVideoResolutionPreset.HD_720, advice.recommendedResolution());
        assertEquals(1, advice.recommendedBatchSize());
        assertTrue(advice.warnings().stream().anyMatch(warning -> warning.contains("4 GB VRAM")));
    }

    @Test
    void twentyFourGbVramAllowsLargerBatches() {
        ProductionHardwareProfileAdvice advice = policy.advise(24576, SimpleVideoResolutionPreset.UHD_4K, 2);

        assertFalse(advice.conservative());
        assertTrue(advice.highCapacity());
        assertEquals(SimpleVideoResolutionPreset.UHD_4K, advice.recommendedResolution());
        assertEquals(4, advice.recommendedBatchSize());
    }
}
