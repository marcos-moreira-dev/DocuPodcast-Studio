package com.marcosmoreiradev.docupodcaststudio.application.narrative;

import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentId;

import java.util.Objects;

/** Fragment-level narrative video readiness view. */
public record NarrativeVisualFragment(
        FragmentId fragmentId,
        int order,
        String segmentId,
        String title,
        String text,
        NarrativeVisualSlot mainImage,
        NarrativeVisualSlot bridgeImage,
        String audioRelativePath,
        double audioDurationSeconds,
        boolean audioReady,
        boolean narratable
) {
    public NarrativeVisualFragment {
        fragmentId = Objects.requireNonNull(fragmentId, "fragmentId");
        order = Math.max(0, order);
        segmentId = normalize(segmentId);
        title = normalize(title).isBlank() ? fragmentId.value() : normalize(title);
        text = normalize(text);
        mainImage = mainImage == null
                ? NarrativeVisualSlot.empty(com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetRole.MAIN_IMAGE)
                : mainImage;
        bridgeImage = bridgeImage == null
                ? NarrativeVisualSlot.empty(com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetRole.BRIDGE_TO_NEXT_FRAGMENT)
                : bridgeImage;
        audioRelativePath = normalize(audioRelativePath).replace('\\', '/');
        audioDurationSeconds = Math.max(0.0, audioDurationSeconds);
        if (!audioRelativePath.isBlank()) {
            audioReady = true;
        }
    }

    public boolean mainImageReady() {
        return mainImage.assigned();
    }

    public boolean bridgeImageReady() {
        return bridgeImage.assigned();
    }

    public boolean exportReady() {
        return narratable && audioReady && mainImageReady();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
