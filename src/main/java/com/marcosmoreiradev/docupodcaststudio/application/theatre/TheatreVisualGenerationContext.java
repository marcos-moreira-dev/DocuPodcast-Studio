package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualConditioningReference;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualConditioningRole;

import java.util.ArrayList;
import java.util.List;

/** Complete, UI-independent context used to generate one theatre visual. */
public record TheatreVisualGenerationContext(
        TheatreImageGenerationUnit unit,
        List<TheatreImageContextAsset> identityReferences,
        List<TheatreImageContextAsset> objectReferences,
        List<TheatreImageContextAsset> environmentReferences,
        TheatreImageContextAsset previousFrame,
        TheatreImageContextAsset nextFrame,
        TheatreImageContextAsset drawnFrame,
        TheatreImageContextAsset cameraGuide,
        TheatreImageContextAsset activeStoryboardFrame,
        String activeStoryboardVariant,
        List<TheatreCharacterGenerationContext> characters,
        String actNotes,
        String sceneNotes
) {
    public TheatreVisualGenerationContext(TheatreImageGenerationUnit unit,
                                          List<TheatreImageContextAsset> identityReferences,
                                          List<TheatreImageContextAsset> objectReferences,
                                          List<TheatreImageContextAsset> environmentReferences,
                                          TheatreImageContextAsset previousFrame,
                                          TheatreImageContextAsset nextFrame,
                                          TheatreImageContextAsset drawnFrame) {
        this(unit, identityReferences, objectReferences, environmentReferences,
                previousFrame, nextFrame, drawnFrame, null, drawnFrame,
                drawnFrame == null ? "" : "drawn", List.of(), "", "");
    }

    public TheatreVisualGenerationContext(TheatreImageGenerationUnit unit,
                                          List<TheatreImageContextAsset> identityReferences,
                                          List<TheatreImageContextAsset> objectReferences,
                                          List<TheatreImageContextAsset> environmentReferences,
                                          TheatreImageContextAsset previousFrame,
                                          TheatreImageContextAsset nextFrame,
                                          TheatreImageContextAsset drawnFrame,
                                          TheatreImageContextAsset cameraGuide) {
        this(unit, identityReferences, objectReferences, environmentReferences,
                previousFrame, nextFrame, drawnFrame, cameraGuide, drawnFrame,
                drawnFrame == null ? "" : "drawn", List.of(), "", "");
    }

    public TheatreVisualGenerationContext {
        identityReferences = copy(identityReferences);
        objectReferences = copy(objectReferences);
        environmentReferences = copy(environmentReferences);
        activeStoryboardVariant = activeStoryboardVariant == null ? "" : activeStoryboardVariant.strip();
        characters = characters == null ? List.of() : List.copyOf(characters);
        actNotes = actNotes == null ? "" : actNotes.strip();
        sceneNotes = sceneNotes == null ? "" : sceneNotes.strip();
    }

    public List<TheatreImageContextAsset> allAssets() {
        ArrayList<TheatreImageContextAsset> result = new ArrayList<>();
        result.addAll(identityReferences);
        result.addAll(objectReferences);
        result.addAll(environmentReferences);
        add(result, previousFrame);
        add(result, nextFrame);
        add(result, drawnFrame);
        add(result, cameraGuide);
        add(result, activeStoryboardFrame);
        return result.stream().filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toMap(
                                asset -> asset.role() + ':' + asset.assetId(),
                                asset -> asset,
                                (first, ignored) -> first,
                                java.util.LinkedHashMap::new),
                        map -> List.copyOf(map.values())));
    }

    public List<VisualConditioningReference> conditioningReferences() {
        ArrayList<VisualConditioningReference> result = new ArrayList<>();
        identityReferences.forEach(asset -> add(result, asset, VisualConditioningRole.IDENTITY, 0.85));
        objectReferences.forEach(asset -> add(result, asset, VisualConditioningRole.OBJECT, 0.55));
        environmentReferences.forEach(asset -> add(result, asset, VisualConditioningRole.ENVIRONMENT, 0.45));
        add(result, cameraGuide, VisualConditioningRole.CAMERA_GUIDE, 0.45);
        add(result, drawnFrame, VisualConditioningRole.DRAWN_GUIDE, 0.30);
        add(result, previousFrame, VisualConditioningRole.PREVIOUS_FRAME, 0.35);
        add(result, nextFrame, VisualConditioningRole.NEXT_FRAME, 0.35);
        if (activeStoryboardFrame != null && drawnFrame == null) {
            add(result, activeStoryboardFrame, VisualConditioningRole.CAMERA_GUIDE, 0.65);
        }
        return List.copyOf(result);
    }

    public boolean requiresIdentityConditioning() {
        return !identityReferences.isEmpty();
    }

    public boolean hasStructureGuide() {
        return (activeStoryboardFrame != null && !activeStoryboardFrame.imageUri().isBlank())
                || (cameraGuide != null && !cameraGuide.imageUri().isBlank());
    }

    public List<TheatreCharacterGenerationContext> missingCharacterIdentities() {
        return characters.stream().filter(character -> !character.identityReady()).toList();
    }

    private static List<TheatreImageContextAsset> copy(List<TheatreImageContextAsset> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static void add(List<TheatreImageContextAsset> target, TheatreImageContextAsset value) {
        if (value != null) target.add(value);
    }

    private static void add(List<VisualConditioningReference> target,
                            TheatreImageContextAsset asset,
                            VisualConditioningRole role,
                            double strength) {
        if (asset == null || asset.imageUri().isBlank()) return;
        try {
            target.add(new VisualConditioningReference(asset.assetId(), asset.label(),
                    java.nio.file.Path.of(java.net.URI.create(asset.imageUri())), role, strength));
        } catch (IllegalArgumentException ignored) {
            // Invalid project references are omitted and reported by the capability preflight.
        }
    }
}
