package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan.ActPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan.ImportPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan.ScenePlan;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TheatreExampleSetupService {

    public TheatreSetupResult execute(ImportPlan plan,
                                      NarrationScriptDocument script,
                                      VoiceLibrary voiceLibrary,
                                      Map<String, String> importedAssetIds) {
        if (plan == null) {
            TheatreAviadoresConfigurator fallback = new TheatreAviadoresConfigurator();
            TheatreAviadoresConfigurator.ConfigureResult r = fallback.execute(script, voiceLibrary, importedAssetIds);
            return new TheatreSetupResult(r.layer(), r.sceneBoundariesStart(), r.sceneBoundariesEnd(), Map.of(), r.emotionAssignments(), List.of());
        }
        TheatreImportUseCase.ImportResult importResult = new TheatreImportUseCase()
                .execute(plan, script, voiceLibrary, importedAssetIds);
        TheatreAviadoresConfigurator fallbackPlacements = new TheatreAviadoresConfigurator();
        TheatreAviadoresConfigurator.ConfigureResult placements = fallbackPlacements.execute(script, voiceLibrary, importedAssetIds);
        List<TheatreProjectLayer.TextActionPlacement> selectedPlacements =
                importResult.layer().textActionPlacements().isEmpty()
                        ? placements.layer().textActionPlacements()
                        : importResult.layer().textActionPlacements();
        TheatreProjectLayer layer = importResult.layer().withTextActionPlacements(selectedPlacements);
        Map<String, String> startBoundaries = importResult.sceneBoundariesStart().isEmpty()
                ? placements.sceneBoundariesStart()
                : importResult.sceneBoundariesStart();
        Map<String, String> endBoundaries = importResult.sceneBoundariesEnd().isEmpty()
                ? placements.sceneBoundariesEnd()
                : importResult.sceneBoundariesEnd();
        return new TheatreSetupResult(layer,
                startBoundaries,
                endBoundaries,
                sceneTextRanges(plan, layer.scenes()),
                importResult.emotionAssignments().isEmpty()
                        ? placements.emotionAssignments()
                        : importResult.emotionAssignments(),
                importResult.imageAssignments());
    }

    private static Map<String, SceneTextRange> sceneTextRanges(ImportPlan plan, List<TheatreProjectLayer.Scene> scenes) {
        if (plan == null || scenes == null || scenes.isEmpty()) {
            return Map.of();
        }
        Map<String, String> sceneIdByName = new LinkedHashMap<>();
        for (TheatreProjectLayer.Scene scene : scenes) {
            sceneIdByName.put(normalize(scene.displayName()), scene.id());
        }
        Map<String, SceneTextRange> ranges = new LinkedHashMap<>();
        for (ActPlan act : plan.acts()) {
            for (ScenePlan scene : act.scenes()) {
                if (scene.textStartIndex() <= 0 || scene.textEndIndex() <= 0) {
                    continue;
                }
                String sceneId = sceneIdByName.get(normalize(scene.name()));
                if (sceneId == null || sceneId.isBlank()) {
                    continue;
                }
                ranges.put(sceneId, new SceneTextRange(scene.textStartIndex(), scene.textEndIndex()));
            }
        }
        return Map.copyOf(ranges);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(java.util.Locale.ROOT);
    }

    public record TheatreSetupResult(
            TheatreProjectLayer layer,
            Map<String, String> sceneBoundariesStart,
            Map<String, String> sceneBoundariesEnd,
            Map<String, SceneTextRange> sceneTextRanges,
            List<NarrativeLayerAssignment> emotionAssignments,
            List<NarrativeLayerAssignment> imageAssignments) {
    }

    public record SceneTextRange(int startTextIndex, int endTextIndex) {
    }
}
