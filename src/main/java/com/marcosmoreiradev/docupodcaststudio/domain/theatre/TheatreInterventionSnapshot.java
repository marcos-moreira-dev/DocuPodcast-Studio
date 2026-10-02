package com.marcosmoreiradev.docupodcaststudio.domain.theatre;

import java.util.List;
import java.util.Map;

/** Complete, immutable and AI-free projection of one theatrical intervention. */
public record TheatreInterventionSnapshot(
        String interventionId,
        int sequence,
        String actId,
        String sceneId,
        String text,
        String speakerCharacterId,
        String voiceId,
        String tone,
        String microexpression,
        String emoji,
        List<String> interactionTargets,
        Map<String, CharacterSnapshot> characters,
        List<TheatreInterventionState.StageEvent> actions,
        Map<String, ObjectSnapshot> objects,
        String backdropId,
        String spatialMapAssetId,
        String cameraId,
        List<String> chorusCharacterIds,
        List<String> audioAssetIds,
        String frameAssetId,
        List<String> intermediateFrameAssetIds,
        List<String> resolvedAssetReferences,
        List<String> invalidReferences
) {
    public TheatreInterventionSnapshot {
        interactionTargets = List.copyOf(interactionTargets);
        characters = Map.copyOf(characters);
        actions = List.copyOf(actions);
        objects = Map.copyOf(objects);
        chorusCharacterIds = List.copyOf(chorusCharacterIds);
        audioAssetIds = List.copyOf(audioAssetIds);
        intermediateFrameAssetIds = List.copyOf(intermediateFrameAssetIds);
        resolvedAssetReferences = List.copyOf(resolvedAssetReferences);
        invalidReferences = List.copyOf(invalidReferences);
    }

    public record CharacterSnapshot(boolean present, String position, String orientation,
                                    String gazeTarget, String visualVariantId, String costume) { }
    public record ObjectSnapshot(boolean present, String position, String holderCharacterId,
                                 String manipulation) { }
}
