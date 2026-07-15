package com.marcosmoreiradev.docupodcaststudio.application.fragment;

import com.marcosmoreiradev.docupodcaststudio.domain.fragment.DocumentFragment;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentId;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/** Read-only cross-media view of fragments and the assets/layers currently attached to them. */
public record FragmentWorkspaceProjection(
        List<DocumentFragment> fragments,
        List<FragmentAssetBinding> bindings
) {
    public FragmentWorkspaceProjection {
        fragments = fragments == null ? List.of() : List.copyOf(fragments);
        bindings = bindings == null ? List.of() : List.copyOf(bindings);
        validate(fragments, bindings);
    }

    public Optional<DocumentFragment> fragmentById(FragmentId fragmentId) {
        if (fragmentId == null) {
            return Optional.empty();
        }
        return fragments.stream().filter(fragment -> fragment.fragmentId().equals(fragmentId)).findFirst();
    }

    public Optional<DocumentFragment> fragmentForBlock(String blockId) {
        String target = normalize(blockId);
        if (target.isBlank()) {
            return Optional.empty();
        }
        return fragments.stream().filter(fragment -> fragment.sourceBlockId().equals(target)).findFirst();
    }

    public Optional<DocumentFragment> fragmentForSegment(String segmentId) {
        String target = normalize(segmentId);
        if (target.isBlank()) {
            return Optional.empty();
        }
        return fragments.stream().filter(fragment -> fragment.segmentId().equals(target)).findFirst();
    }

    public List<FragmentAssetBinding> bindingsForFragment(FragmentId fragmentId) {
        if (fragmentId == null) {
            return List.of();
        }
        return bindings.stream()
                .filter(binding -> binding.fragmentId().equals(fragmentId))
                .toList();
    }

    private static void validate(List<DocumentFragment> fragments, List<FragmentAssetBinding> bindings) {
        LinkedHashSet<FragmentId> fragmentIds = new LinkedHashSet<>();
        for (DocumentFragment fragment : fragments) {
            if (!fragmentIds.add(fragment.fragmentId())) {
                throw new IllegalArgumentException("Fragmento duplicado: " + fragment.fragmentId());
            }
        }
        LinkedHashSet<String> bindingIds = new LinkedHashSet<>();
        for (FragmentAssetBinding binding : bindings) {
            if (!bindingIds.add(binding.id())) {
                throw new IllegalArgumentException("Binding duplicado: " + binding.id());
            }
            if (!fragmentIds.contains(binding.fragmentId())) {
                throw new IllegalArgumentException("Binding apunta a fragmento inexistente: " + binding.fragmentId());
            }
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
