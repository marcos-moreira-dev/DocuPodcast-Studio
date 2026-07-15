package com.marcosmoreiradev.docupodcaststudio.application.audio;

import java.util.List;

/** Aggregate readiness for audio and voice production across fragments. */
public record AudioVoiceReadiness(
        int totalFragments,
        int narratableFragments,
        int audioReadyCount,
        int audioMissingCount,
        int voiceReadyCount,
        int unavailableVoiceCount,
        boolean engineReady,
        List<String> blockers,
        List<String> warnings
) {
    public AudioVoiceReadiness {
        totalFragments = Math.max(0, totalFragments);
        narratableFragments = Math.max(0, narratableFragments);
        audioReadyCount = Math.max(0, audioReadyCount);
        audioMissingCount = Math.max(0, audioMissingCount);
        voiceReadyCount = Math.max(0, voiceReadyCount);
        unavailableVoiceCount = Math.max(0, unavailableVoiceCount);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public boolean readyForExport() {
        return narratableFragments > 0 && audioMissingCount == 0 && unavailableVoiceCount == 0 && engineReady;
    }
}
