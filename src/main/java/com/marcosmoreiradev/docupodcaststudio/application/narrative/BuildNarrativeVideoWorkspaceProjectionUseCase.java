package com.marcosmoreiradev.docupodcaststudio.application.narrative;

import com.marcosmoreiradev.docupodcaststudio.application.fragment.FragmentWorkspaceProjection;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.DocumentFragment;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetRole;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetSource;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Builds the Video narrativo view from the canonical fragment workspace projection. */
public final class BuildNarrativeVideoWorkspaceProjectionUseCase {
    public NarrativeVideoWorkspaceProjection build(FragmentWorkspaceProjection projection) {
        if (projection == null) {
            return new NarrativeVideoWorkspaceProjection(List.of());
        }
        ArrayList<NarrativeVisualFragment> fragments = new ArrayList<>();
        for (DocumentFragment fragment : projection.fragments()) {
            List<FragmentAssetBinding> bindings = projection.bindingsForFragment(fragment.fragmentId());
            FragmentAssetBinding main = selectMainImage(bindings);
            FragmentAssetBinding bridge = selectFirst(bindings, FragmentAssetRole.BRIDGE_TO_NEXT_FRAGMENT);
            FragmentAssetBinding audio = selectAudio(bindings);
            fragments.add(new NarrativeVisualFragment(
                    fragment.fragmentId(),
                    fragment.order(),
                    fragment.segmentId(),
                    fragment.sourceLocation().isBlank() ? fragment.fragmentId().value() : fragment.sourceLocation(),
                    fragment.text(),
                    slot(FragmentAssetRole.MAIN_IMAGE, main),
                    slot(FragmentAssetRole.BRIDGE_TO_NEXT_FRAGMENT, bridge),
                    audio == null ? "" : audio.assetPath(),
                    durationSeconds(audio),
                    audio != null && !audio.assetPath().isBlank(),
                    fragment.hasSegment() && fragment.status() != FragmentStatus.VISUAL_ONLY
            ));
        }
        return new NarrativeVideoWorkspaceProjection(fragments);
    }

    private static FragmentAssetBinding selectMainImage(List<FragmentAssetBinding> bindings) {
        return bindings.stream()
                .filter(binding -> binding.role() == FragmentAssetRole.MAIN_IMAGE
                        || binding.role() == FragmentAssetRole.DOCUMENT_IMAGE)
                .min(Comparator.comparingInt(BuildNarrativeVideoWorkspaceProjectionUseCase::mainPriority))
                .orElse(null);
    }

    private static int mainPriority(FragmentAssetBinding binding) {
        if (binding.role() == FragmentAssetRole.MAIN_IMAGE && binding.source() == FragmentAssetSource.NARRATIVE_LAYER) {
            return 0;
        }
        if (binding.role() == FragmentAssetRole.MAIN_IMAGE && binding.source() == FragmentAssetSource.STORYBOARD) {
            return 1;
        }
        if (binding.role() == FragmentAssetRole.DOCUMENT_IMAGE) {
            return 2;
        }
        return 9;
    }

    private static FragmentAssetBinding selectAudio(List<FragmentAssetBinding> bindings) {
        return bindings.stream()
                .filter(binding -> binding.role() == FragmentAssetRole.AUDIO_TTS
                        || binding.role() == FragmentAssetRole.AUDIO_RECORDED
                        || binding.role() == FragmentAssetRole.AUDIO_IMPORTED)
                .filter(binding -> !binding.assetPath().isBlank())
                .findFirst()
                .orElse(null);
    }

    private static FragmentAssetBinding selectFirst(List<FragmentAssetBinding> bindings, FragmentAssetRole role) {
        return bindings.stream()
                .filter(binding -> binding.role() == role)
                .findFirst()
                .orElse(null);
    }

    private static NarrativeVisualSlot slot(FragmentAssetRole role, FragmentAssetBinding binding) {
        if (binding == null) {
            return NarrativeVisualSlot.empty(role);
        }
        return new NarrativeVisualSlot(
                role,
                binding.id(),
                binding.assetId(),
                binding.assetPath(),
                binding.source(),
                binding.metadata().getOrDefault("visualPrompt", ""),
                binding.metadata().getOrDefault("notes", binding.metadata().getOrDefault("caption", ""))
        );
    }

    private static double durationSeconds(FragmentAssetBinding audio) {
        if (audio == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(audio.metadata().getOrDefault("durationSeconds", "0"));
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }
}
