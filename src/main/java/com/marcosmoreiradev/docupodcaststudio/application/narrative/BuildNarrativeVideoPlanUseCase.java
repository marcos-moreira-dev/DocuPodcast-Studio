package com.marcosmoreiradev.docupodcaststudio.application.narrative;

import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoFrame;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPlan;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Converts the narrative fragment projection into the existing simple video contract. */
public final class BuildNarrativeVideoPlanUseCase {
    public static final double DEFAULT_BRIDGE_SECONDS = 2.0;

    public SimpleVideoPlan build(String title, NarrativeVideoWorkspaceProjection projection) {
        return build(title, projection, DEFAULT_BRIDGE_SECONDS);
    }

    public SimpleVideoPlan build(String title, NarrativeVideoWorkspaceProjection projection, double bridgeSeconds) {
        List<NarrativeVisualFragment> fragments = projection == null ? List.of() : projection.fragments();
        ArrayList<SimpleVideoFrame> frames = new ArrayList<>();
        int index = 1;
        for (NarrativeVisualFragment fragment : fragments) {
            if (!fragment.narratable()) {
                continue;
            }
            frames.add(spokenFrame(index++, fragment));
            if (fragment.bridgeImageReady()) {
                frames.add(bridgeFrame(index++, fragment, bridgeSeconds));
            }
        }
        return new SimpleVideoPlan(title == null || title.isBlank() ? "Video narrativo" : title,
                frames, 0.0, Instant.now());
    }

    private static SimpleVideoFrame spokenFrame(int index, NarrativeVisualFragment fragment) {
        NarrativeVisualSlot main = fragment.mainImage();
        return new SimpleVideoFrame(
                frameId(index),
                fragment.segmentId(),
                fragment.title(),
                fragment.text(),
                main.assetId(),
                main.assetPath(),
                fragment.audioRelativePath(),
                fragment.audioDurationSeconds(),
                0.0,
                main.assigned(),
                fragment.audioReady()
        );
    }

    private static SimpleVideoFrame bridgeFrame(int index, NarrativeVisualFragment fragment, double bridgeSeconds) {
        NarrativeVisualSlot bridge = fragment.bridgeImage();
        return new SimpleVideoFrame(
                frameId(index),
                fragment.segmentId() + "-BRIDGE",
                "Puente " + fragment.title(),
                "",
                bridge.assetId(),
                bridge.assetPath(),
                "",
                0.0,
                bridgeSeconds <= 0 ? DEFAULT_BRIDGE_SECONDS : bridgeSeconds,
                bridge.assigned(),
                false,
                true
        );
    }

    private static String frameId(int index) {
        return "FRAME-" + String.format(Locale.ROOT, "%03d", index);
    }
}
