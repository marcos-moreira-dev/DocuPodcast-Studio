package com.marcosmoreiradev.docupodcaststudio.application.storyboard;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDisplayMode;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds or refreshes a storyboard projection from real IMAGE narrative layers.
 *
 * <p>In V1 the storyboard is a layer of the narrated document: the source document remains
 * read-only, image layers live in the DocuPodcast project, and each exported frame lasts for
 * the spoken duration of the associated narration segment.</p>
 */
public final class BuildStoryboardFromImageLayersUseCase {
    public StoryboardDocument build(NarrationScriptDocument script,
                                    StoryboardDocument existingStoryboard,
                                    ProjectAssetCatalog assets,
                                    List<NarrativeLayerAssignment> assignments) {
        Objects.requireNonNull(script, "script");
        ProjectAssetCatalog catalog = assets == null ? ProjectAssetCatalog.empty() : assets;
        StoryboardDocument storyboard = existingStoryboard == null
                ? StoryboardDocument.createForScript(script)
                : existingStoryboard;
        if (assignments == null || assignments.isEmpty()) {
            return storyboard;
        }

        List<NarrativeLayerAssignment> imageLayers = assignments.stream()
                .filter(assignment -> assignment.kind() == NarrativeLayerKind.IMAGE)
                .sorted(Comparator.comparing(assignment -> assignment.textRange().segmentId()))
                .toList();

        StoryboardDocument next = storyboard;
        for (NarrativeLayerAssignment layer : imageLayers) {
            String segmentId = layer.textRange().segmentId();
            if (script.segmentById(segmentId).isEmpty()) {
                continue;
            }
            if (!isVisualAsset(catalog, layer.targetId())) {
                continue;
            }
            String bindingId = "STB-" + segmentId.replaceFirst("^SEG-", "");
            String caption = layer.displayName().isBlank() ? "Imagen asociada al documento" : layer.displayName();
            StoryboardBinding existing = next.bindingForSegment(segmentId).orElse(null);
            Map<String, String> metadata = mergedMetadata(existing, layer, layer.targetId());
            String drawnFrameAssetId = metadata.getOrDefault(UpsertTheatreStoryboardFrameVariantUseCase.DRAWN_FRAME_ASSET_ID, "");
            String generatedImageAssetId = metadata.getOrDefault(UpsertTheatreStoryboardFrameVariantUseCase.GENERATED_IMAGE_ASSET_ID, "");
            String activeVariant = metadata.getOrDefault(UpsertTheatreStoryboardFrameVariantUseCase.ACTIVE_VISUAL_VARIANT,
                    UpsertTheatreStoryboardFrameVariantUseCase.VARIANT_OFFICIAL);
            String activeImageAssetId = switch (TheatreVisualVariant.fromMetadata(activeVariant)) {
                case DRAWN -> isVisualAsset(catalog, drawnFrameAssetId) ? drawnFrameAssetId : layer.targetId();
                case GENERATED -> isVisualAsset(catalog, generatedImageAssetId) ? generatedImageAssetId : layer.targetId();
                case OFFICIAL -> layer.targetId();
            };
            StoryboardBinding binding = new StoryboardBinding(
                    existing == null ? bindingId : existing.id(),
                    segmentId,
                    activeImageAssetId,
                    existing == null ? StoryboardDisplayMode.FIT_CONTAIN : existing.displayMode(),
                    activeImageAssetId.equals(layer.targetId()) ? caption
                            : TheatreVisualVariant.fromMetadata(activeVariant).displayName(),
                    metadata
            );
            next = next.withBinding(binding);
        }
        return next;
    }

    private static Map<String, String> mergedMetadata(StoryboardBinding existing,
                                                       NarrativeLayerAssignment layer,
                                                       String officialAssetId) {
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>(existing == null ? Map.of() : existing.metadata());
        metadata.put("source", "narrative-layer");
        metadata.put("layerId", layer.id());
        metadata.put("duration", "spoken-segment");
        metadata.put(UpsertTheatreStoryboardFrameVariantUseCase.OFFICIAL_IMAGE_ASSET_ID, officialAssetId);
        metadata.putIfAbsent(UpsertTheatreStoryboardFrameVariantUseCase.ACTIVE_VISUAL_VARIANT,
                UpsertTheatreStoryboardFrameVariantUseCase.VARIANT_OFFICIAL);
        return Map.copyOf(metadata);
    }

    private static boolean isVisualAsset(ProjectAssetCatalog catalog, String assetId) {
        return catalog.byId(assetId)
                .filter(asset -> asset.kind() == ProjectAssetKind.IMAGE || asset.kind() == ProjectAssetKind.THUMBNAIL)
                .isPresent();
    }
}
