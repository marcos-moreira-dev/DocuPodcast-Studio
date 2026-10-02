package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.storyboard.UpsertTheatreStoryboardFrameVariantUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Applies the canonical theatre primary-image priority: user image, generated image, storyboard frame, legacy visual. */
public final class TheatrePrimaryVisualResolver {
    public Optional<TheatrePrimaryVisualReference> resolve(DocuPodcastProject project,
                                                           StoryboardDocument storyboard,
                                                           NarrationScriptDocument script,
                                                           String interventionId,
                                                           Path projectDirectory) {
        if (project == null || project.theatre() == null || interventionId == null || interventionId.isBlank()) {
            return Optional.empty();
        }
        Optional<NarrationSegment> segment = segmentForIntervention(project.theatre(), script, interventionId);
        if (segment.isPresent()) {
            Optional<TheatrePrimaryVisualReference> resolved = resolveForSegment(project, storyboard,
                    segment.get(), projectDirectory);
            if (resolved.isPresent()) {
                return resolved;
            }
        }
        return legacyVisual(project, interventionId, "", projectDirectory);
    }

    public Optional<TheatrePrimaryVisualReference> resolveForSegment(DocuPodcastProject project,
                                                                     StoryboardDocument storyboard,
                                                                     NarrationSegment segment,
                                                                     Path projectDirectory) {
        if (project == null || segment == null) {
            return Optional.empty();
        }
        String interventionId = interventionForSegment(project.theatre(), segment).orElse("");
        ProjectAssetCatalog assets = project.assets() == null ? ProjectAssetCatalog.empty() : project.assets();
        StoryboardBinding binding = storyboard == null ? null : storyboard.bindingForSegment(segment.id()).orElse(null);
        if (binding != null) {
            Map<String, String> metadata = binding.metadata();
            String activeVariant = metadata.getOrDefault(
                    UpsertTheatreStoryboardFrameVariantUseCase.ACTIVE_VISUAL_VARIANT,
                    UpsertTheatreStoryboardFrameVariantUseCase.VARIANT_OFFICIAL);
            Candidate official = new Candidate(metadata.getOrDefault(
                    UpsertTheatreStoryboardFrameVariantUseCase.OFFICIAL_IMAGE_ASSET_ID, ""),
                    TheatrePrimaryVisualReference.Source.OFFICIAL_IMAGE);
            Candidate generated = new Candidate(metadata.getOrDefault(
                    UpsertTheatreStoryboardFrameVariantUseCase.GENERATED_IMAGE_ASSET_ID, ""),
                    TheatrePrimaryVisualReference.Source.GENERATED_IMAGE);
            Candidate drawn = new Candidate(metadata.getOrDefault(
                    UpsertTheatreStoryboardFrameVariantUseCase.DRAWN_FRAME_ASSET_ID, ""),
                    TheatrePrimaryVisualReference.Source.STORYBOARD_FRAME);
            Candidate scenery = new Candidate(metadata.getOrDefault(
                    UpsertTheatreStoryboardFrameVariantUseCase.SCENERY_IMAGE_ASSET_ID, ""),
                    TheatrePrimaryVisualReference.Source.SCENERY_COMPOSITION);
            LinkedHashMap<TheatrePrimaryVisualReference.Source, Candidate> ordered = new LinkedHashMap<>();
            if (isActive(activeVariant, UpsertTheatreStoryboardFrameVariantUseCase.VARIANT_SCENERY)) {
                ordered.put(scenery.source(), new Candidate(
                        firstNonBlank(scenery.assetId(), binding.imageAssetId()), scenery.source()));
            }
            for (Candidate fallback : List.of(official, generated, drawn, scenery)) {
                ordered.putIfAbsent(fallback.source(), fallback);
            }
            for (Candidate candidate : ordered.values()) {
                Optional<TheatrePrimaryVisualReference> resolved = usable(assets, candidate.assetId(),
                        projectDirectory, interventionId, segment.id(), candidate.source());
                if (resolved.isPresent()) {
                    return resolved;
                }
            }
        }
        return legacyVisual(project, interventionId, segment.id(), projectDirectory);
    }

    private static Optional<NarrationSegment> segmentForIntervention(TheatreProjectLayer theatre,
                                                                     NarrationScriptDocument script,
                                                                     String interventionId) {
        if (theatre == null || script == null || script.empty()) {
            return Optional.empty();
        }
        return theatre.intervenciones().stream()
                .filter(intervention -> intervention.id().equals(interventionId))
                .findFirst()
                .flatMap(intervention -> script.segments().stream()
                        .filter(NarrationSegment::narratable)
                        .filter(segment -> segment.sourceBlockIds().contains(intervention.blockId()))
                        .findFirst());
    }

    private static Optional<String> interventionForSegment(TheatreProjectLayer theatre, NarrationSegment segment) {
        if (theatre == null || segment == null) {
            return Optional.empty();
        }
        return theatre.intervenciones().stream()
                .filter(intervention -> segment.sourceBlockIds().contains(intervention.blockId()))
                .map(TheatreProjectLayer.Intervencion::id)
                .findFirst();
    }

    private Optional<TheatrePrimaryVisualReference> legacyVisual(DocuPodcastProject project,
                                                                 String interventionId,
                                                                 String segmentId,
                                                                 Path projectDirectory) {
        if (project == null || project.theatre() == null || interventionId == null || interventionId.isBlank()) {
            return Optional.empty();
        }
        ProjectAssetCatalog assets = project.assets() == null ? ProjectAssetCatalog.empty() : project.assets();
        return project.theatre().intervencionesVisuales().stream()
                .filter(visual -> interventionId.equals(visual.intervencionId()))
                .findFirst()
                .flatMap(visual -> usable(assets, visual.assetId(), projectDirectory, interventionId, segmentId,
                        TheatrePrimaryVisualReference.Source.LEGACY_THEATRE_VISUAL));
    }

    private static Optional<TheatrePrimaryVisualReference> usable(ProjectAssetCatalog assets,
                                                                  String assetId,
                                                                  Path projectDirectory,
                                                                  String interventionId,
                                                                  String segmentId,
                                                                  TheatrePrimaryVisualReference.Source source) {
        if (assets == null || assetId == null || assetId.isBlank() || projectDirectory == null) {
            return Optional.empty();
        }
        Path root = projectDirectory.toAbsolutePath().normalize();
        return assets.byId(assetId)
                .filter(ProjectAssetReference::isImage)
                .flatMap(asset -> {
                    Path path = root.resolve(asset.relativePath()).toAbsolutePath().normalize();
                    if (!path.startsWith(root) || !Files.isRegularFile(path)) {
                        return Optional.empty();
                    }
                    return Optional.of(new TheatrePrimaryVisualReference(interventionId, segmentId, asset, path, source));
                });
    }

    private static boolean isActive(String activeVariant, String expected) {
        return expected.equalsIgnoreCase(activeVariant == null ? "" : activeVariant.strip());
    }

    private static String firstNonBlank(String first, String second) {
        String a = first == null ? "" : first.strip();
        return a.isBlank() ? (second == null ? "" : second.strip()) : a;
    }

    private record Candidate(String assetId, TheatrePrimaryVisualReference.Source source) {
    }
}
