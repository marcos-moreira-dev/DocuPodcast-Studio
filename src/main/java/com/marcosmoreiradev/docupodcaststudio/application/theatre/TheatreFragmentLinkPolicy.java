package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.fragment.FragmentWorkspaceProjection;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.DocumentFragment;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentId;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Resolves theatre selections without adding persistent identifiers to the theatre layer. */
public final class TheatreFragmentLinkPolicy {
    public TheatreFragmentLink linkForIntervention(
            TheatreProjectLayer theatre,
            TheatreProjectLayer.Intervencion intervention,
            FragmentWorkspaceProjection projection
    ) {
        if (intervention == null) {
            throw new IllegalArgumentException("intervention is required");
        }
        FragmentId fragmentId = FragmentId.fromBlockId(intervention.blockId());
        Optional<DocumentFragment> fragment = projection == null
                ? Optional.empty()
                : projection.fragmentById(fragmentId);
        return new TheatreFragmentLink(
                intervention.id(),
                intervention.blockId(),
                fragmentId,
                fragment.map(DocumentFragment::segmentId).orElse(""),
                sceneIdForIntervention(theatre, intervention.id()).orElse(""),
                fragment.isPresent());
    }

    public Optional<TheatreProjectLayer.Intervencion> interventionForFragment(
            TheatreProjectLayer theatre,
            FragmentId fragmentId
    ) {
        if (theatre == null || fragmentId == null) {
            return Optional.empty();
        }
        return theatre.intervenciones().stream()
                .filter(intervention -> FragmentId.fromBlockId(intervention.blockId()).equals(fragmentId))
                .min(Comparator.comparingInt(TheatreProjectLayer.Intervencion::sequenceIndex));
    }

    public Optional<TheatreProjectLayer.Intervencion> interventionForBlock(
            TheatreProjectLayer theatre,
            String blockId
    ) {
        String target = normalize(blockId);
        if (theatre == null || target.isBlank()) {
            return Optional.empty();
        }
        return theatre.intervenciones().stream()
                .filter(intervention -> intervention.blockId().equals(target))
                .min(Comparator.comparingInt(TheatreProjectLayer.Intervencion::sequenceIndex));
    }

    public Optional<String> sceneForFragment(TheatreProjectLayer theatre, FragmentId fragmentId) {
        return interventionForFragment(theatre, fragmentId)
                .flatMap(intervention -> sceneIdForIntervention(theatre, intervention.id()));
    }

    public Optional<String> sceneIdForIntervention(TheatreProjectLayer theatre, String interventionId) {
        String target = normalize(interventionId);
        if (theatre == null || target.isBlank()) {
            return Optional.empty();
        }
        Optional<String> placementScene = theatre.textActionPlacements().stream()
                .filter(placement -> placement.intervencionId().equals(target))
                .map(TheatreProjectLayer.TextActionPlacement::sceneId)
                .filter(sceneId -> !sceneId.isBlank())
                .findFirst();
        if (placementScene.isPresent()) {
            return placementScene;
        }
        Optional<String> positionScene = theatre.positions().stream()
                .filter(position -> position.alias().equals(target))
                .map(TheatreProjectLayer.SpatialPosition::sceneId)
                .filter(sceneId -> !sceneId.isBlank())
                .findFirst();
        if (positionScene.isPresent()) {
            return positionScene;
        }
        return theatre.actions().stream()
                .filter(action -> action.fromAlias().equals(target) || action.toAlias().equals(target))
                .map(TheatreProjectLayer.TheatreAction::sceneId)
                .filter(sceneId -> !sceneId.isBlank())
                .findFirst();
    }

    public List<TheatreProjectLayer.Intervencion> interventionsForScene(TheatreProjectLayer theatre, String sceneId) {
        String target = normalize(sceneId);
        if (theatre == null || target.isBlank()) {
            return List.of();
        }
        return theatre.intervenciones().stream()
                .filter(intervention -> sceneIdForIntervention(theatre, intervention.id())
                        .map(target::equals)
                        .orElse(false))
                .sorted(Comparator.comparingInt(TheatreProjectLayer.Intervencion::sequenceIndex))
                .toList();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
