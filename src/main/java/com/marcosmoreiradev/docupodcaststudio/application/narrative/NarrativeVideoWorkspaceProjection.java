package com.marcosmoreiradev.docupodcaststudio.application.narrative;

import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentId;

import java.util.List;
import java.util.Optional;

/** Ordered Video narrativo workspace projection derived from the canonical fragment view. */
public record NarrativeVideoWorkspaceProjection(
        List<NarrativeVisualFragment> fragments
) {
    public NarrativeVideoWorkspaceProjection {
        fragments = fragments == null ? List.of() : List.copyOf(fragments);
    }

    public Optional<NarrativeVisualFragment> fragmentById(FragmentId fragmentId) {
        if (fragmentId == null) {
            return Optional.empty();
        }
        return fragments.stream().filter(fragment -> fragment.fragmentId().equals(fragmentId)).findFirst();
    }

    public long narratableCount() {
        return fragments.stream().filter(NarrativeVisualFragment::narratable).count();
    }

    public long readyForExportCount() {
        return fragments.stream().filter(NarrativeVisualFragment::exportReady).count();
    }

    public long missingMainImageCount() {
        return fragments.stream()
                .filter(NarrativeVisualFragment::narratable)
                .filter(fragment -> !fragment.mainImageReady())
                .count();
    }

    public long missingAudioCount() {
        return fragments.stream()
                .filter(NarrativeVisualFragment::narratable)
                .filter(fragment -> !fragment.audioReady())
                .count();
    }

    public long bridgeImageCount() {
        return fragments.stream().filter(NarrativeVisualFragment::bridgeImageReady).count();
    }
}
