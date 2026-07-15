package com.marcosmoreiradev.docupodcaststudio.application.compute;

import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoResolutionPreset;

import java.util.List;

/** Human-facing advice for production profiles on limited or strong hardware. */
public record ProductionHardwareProfileAdvice(
        String profileName,
        long detectedVramMb,
        SimpleVideoResolutionPreset recommendedResolution,
        int recommendedBatchSize,
        boolean conservative,
        boolean highCapacity,
        List<String> warnings
) {
    public ProductionHardwareProfileAdvice {
        profileName = profileName == null || profileName.isBlank() ? "Perfil conservador" : profileName.strip();
        detectedVramMb = Math.max(0L, detectedVramMb);
        recommendedResolution = recommendedResolution == null ? SimpleVideoResolutionPreset.HD_720 : recommendedResolution;
        recommendedBatchSize = Math.max(1, recommendedBatchSize);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
