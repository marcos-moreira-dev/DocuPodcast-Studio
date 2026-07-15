package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Plans which A/B theatre intermediate frames can be generated from existing principal visuals. */
public final class TheatreIntermediateFrameBatchPlanner {
    private final TheatrePrimaryVisualResolver visualResolver = new TheatrePrimaryVisualResolver();
    private final TheatreVisualContinuityResolver continuityResolver = new TheatreVisualContinuityResolver();

    public Plan plan(DocuPodcastProject project,
                     StoryboardDocument storyboard,
                     NarrationScriptDocument script,
                     List<TheatreImageGenerationUnit> units,
                     Path projectDirectory) {
        List<TheatreImageGenerationUnit> safeUnits = units == null ? List.of() : units;
        Map<String, String> existing = existingIntermediatePairs(project, projectDirectory);
        ArrayList<Item> missingGenerable = new ArrayList<>();
        ArrayList<Item> allGenerable = new ArrayList<>();
        ArrayList<Item> overwriteGenerable = new ArrayList<>();
        LinkedHashSet<String> missingPrincipalInterventions = new LinkedHashSet<>();
        int totalPairs = Math.max(0, safeUnits.size() - 1);
        int existingPairs = 0;
        int blockedPairs = 0;
        for (int i = 0; i + 1 < safeUnits.size(); i++) {
            TheatreImageGenerationUnit current = safeUnits.get(i);
            TheatreImageGenerationUnit next = safeUnits.get(i + 1);
            String key = pairKey(current.interventionId(), next.interventionId());
            String existingAssetId = existing.get(key);
            boolean existingPair = existingAssetId != null;
            if (!continuityResolver.canInterpolate(project == null ? null : project.theatre(),
                    current.interventionId(), next.interventionId())) {
                blockedPairs++;
                continue;
            }
            if (existingPair) {
                existingPairs++;
            }
            var previousReference = visualResolver.resolve(project, storyboard, script,
                    current.interventionId(), projectDirectory);
            var nextReference = visualResolver.resolve(project, storyboard, script,
                    next.interventionId(), projectDirectory);
            if (previousReference.isEmpty() || nextReference.isEmpty()) {
                blockedPairs++;
                if (previousReference.isEmpty()) {
                    missingPrincipalInterventions.add(current.interventionId());
                }
                if (nextReference.isEmpty()) {
                    missingPrincipalInterventions.add(next.interventionId());
                }
                continue;
            }
            Item item = new Item(current, next, previousReference.get(), nextReference.get(),
                    existingPair, existingAssetId == null ? "" : existingAssetId);
            allGenerable.add(item);
            if (existingPair) {
                overwriteGenerable.add(item);
            } else {
                missingGenerable.add(item);
            }
        }
        return new Plan(totalPairs, existingPairs, missingGenerable.size(), allGenerable.size(), blockedPairs,
                List.copyOf(missingPrincipalInterventions), List.copyOf(missingGenerable),
                List.copyOf(allGenerable), List.copyOf(overwriteGenerable));
    }

    private static Map<String, String> existingIntermediatePairs(DocuPodcastProject project, Path projectDirectory) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        if (project == null || project.theatre() == null || projectDirectory == null) {
            return result;
        }
        Path root = projectDirectory.toAbsolutePath().normalize();
        for (TheatreProjectLayer.IntermediateFrame frame : project.theatre().intermediateFrames()) {
            project.assets().byId(frame.assetId())
                    .filter(asset -> {
                        Path path = root.resolve(asset.relativePath()).toAbsolutePath().normalize();
                        return path.startsWith(root) && Files.isRegularFile(path);
                    })
                    .ifPresent(asset -> result.put(pairKey(frame.fromIntervencionId(), frame.toIntervencionId()),
                            asset.id()));
        }
        return result;
    }

    private static String pairKey(String fromIntervencionId, String toIntervencionId) {
        return (fromIntervencionId == null ? "" : fromIntervencionId) + "->"
                + (toIntervencionId == null ? "" : toIntervencionId);
    }

    public record Plan(
            int totalPairs,
            int existingPairs,
            int generablePairs,
            int allGenerablePairs,
            int blockedPairs,
            List<String> missingPrincipalInterventionIds,
            List<Item> missingItems,
            List<Item> allGenerableItems,
            List<Item> overwriteItems
    ) {
        public Plan {
            missingPrincipalInterventionIds = missingPrincipalInterventionIds == null
                    ? List.of()
                    : List.copyOf(missingPrincipalInterventionIds);
            missingItems = missingItems == null ? List.of() : List.copyOf(missingItems);
            allGenerableItems = allGenerableItems == null ? List.of() : List.copyOf(allGenerableItems);
            overwriteItems = overwriteItems == null ? List.of() : List.copyOf(overwriteItems);
        }

        public int missingPrincipalInterventions() {
            return missingPrincipalInterventionIds.size();
        }

        public int overwritePairs() {
            return overwriteItems.size();
        }

        public List<Item> items() {
            return missingItems;
        }

        public boolean hasGenerablePairs() {
            return hasMissingGenerablePairs();
        }

        public boolean hasMissingGenerablePairs() {
            return generablePairs > 0 && !missingItems.isEmpty();
        }

        public boolean hasAllGenerablePairs() {
            return allGenerablePairs > 0 && !allGenerableItems.isEmpty();
        }
    }

    public record Item(
            TheatreImageGenerationUnit current,
            TheatreImageGenerationUnit next,
            TheatrePrimaryVisualReference previousReference,
            TheatrePrimaryVisualReference nextReference,
            boolean existingIntermediate,
            String existingIntermediateAssetId
    ) {
        public Item {
            existingIntermediateAssetId = existingIntermediateAssetId == null ? "" : existingIntermediateAssetId;
        }
    }
}
