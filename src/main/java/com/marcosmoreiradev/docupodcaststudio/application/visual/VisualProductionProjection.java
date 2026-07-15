package com.marcosmoreiradev.docupodcaststudio.application.visual;

import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentId;

import java.util.List;
import java.util.Optional;

/** Transversal visual production projection derived from fragments and project assets. */
public record VisualProductionProjection(
        List<VisualFragmentState> fragments,
        List<VisualSlotState> globalVisuals,
        VisualReadiness readiness,
        List<VisualDiagnostic> diagnostics
) {
    public VisualProductionProjection {
        fragments = fragments == null ? List.of() : List.copyOf(fragments);
        globalVisuals = globalVisuals == null ? List.of() : List.copyOf(globalVisuals);
        readiness = readiness == null
                ? new VisualReadiness(0, 0, 0, 0, 0, 0, 0, 0, 0, List.of(), List.of())
                : readiness;
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public Optional<VisualFragmentState> fragmentById(FragmentId fragmentId) {
        if (fragmentId == null) {
            return Optional.empty();
        }
        return fragments.stream().filter(fragment -> fragment.fragmentId().equals(fragmentId)).findFirst();
    }
}
