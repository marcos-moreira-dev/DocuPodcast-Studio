package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentSentenceSpan;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentSentenceSplitter;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.UpsertTheatreStoryboardFrameVariantUseCase;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Builds document-first rail projections without forcing JavaFX views to know project internals. */
public final class DocumentRailProjectionFactory {
    private DocumentRailProjectionFactory() {}

    public static List<DocumentFragmentRailPresentation> fragments(ReadableDocument document,
                                                                   NarrationScriptDocument script,
                                                                   DocuPodcastProject project,
                                                                   StoryboardDocument storyboard,
                                                                   Optional<Path> projectDirectory) {
        if (document == null || script == null || script.empty()) return List.of();
        ArrayList<DocumentFragmentRailPresentation> result = new ArrayList<>();
        for (NarrationSegment segment : script.segments()) {
            Optional<String> blockId = firstSourceBlockId(segment);
            if (blockId.isEmpty()) continue;
            Optional<DocumentBlock> block = document.blockById(blockId.get());
            if (block.isEmpty() || !block.get().narratable()) continue;
            List<DocumentSentenceSpan> spans = DocumentSentenceSplitter.split(block.get());
            if (spans.isEmpty()) continue;
            for (DocumentSentenceSpan span : spans) {
                String unitId = segment.id() + "-U" + String.format("%03d", span.index() + 1);
                ScriptTextRange scriptRange = scriptRange(segment, span.range(), span.text());
                Optional<NarrativeLayerAssignment> imageLayer = imageAssignment(project, scriptRange);
                Optional<ProjectAssetReference> image = imageLayer.flatMap(layer -> imageAsset(project, layer.targetId()));
                StoryboardVisual visual = storyboardVisual(project, storyboard, segment.id(), image.orElse(null), projectDirectory);
                result.add(new DocumentFragmentRailPresentation(
                        unitId,
                        segment.id(),
                        span.blockId(),
                        span.range().startOffset(),
                        span.range().endOffset(),
                        span.text(),
                        span.text(),
                        visual.activeAssetId(),
                        visual.activeUri(),
                        visual.officialAssetId(),
                        visual.officialUri(),
                        visual.generatedAssetId(),
                        visual.generatedUri(),
                        visual.drawnFrameAssetId(),
                        visual.drawnFrameUri(),
                        visual.sceneryAssetId(),
                        visual.sceneryUri(),
                        visual.activeVariant()));
            }
        }
        return List.copyOf(result);
    }

    public static List<DocumentRailImagePresentation> images(DocuPodcastProject project,
                                                             StoryboardDocument storyboard,
                                                             Optional<Path> projectDirectory,
                                                             java.util.function.Function<String, Optional<NarrationSegment>> segmentFinder,
                                                             java.util.function.Function<NarrationSegment, Optional<String>> blockFinder) {
        if (project == null) return List.of();
        List<ProjectAssetReference> images = project.assets().byKind(ProjectAssetKind.IMAGE);
        if (images.isEmpty()) return List.of();
        Map<String, List<StoryboardBinding>> bindingsByImage = storyboard == null ? Map.of() : storyboard.bindingsByImageAssetId();
        ArrayList<DocumentRailImagePresentation> result = new ArrayList<>();
        for (ProjectAssetReference image : images) {
            List<StoryboardBinding> bindings = bindingsByImage.getOrDefault(image.id(), List.of());
            StoryboardBinding binding = bindings.isEmpty() ? null : bindings.get(0);
            String segmentId = binding == null ? "" : binding.segmentId();
            Optional<NarrationSegment> segment = segmentFinder.apply(segmentId);
            result.add(new DocumentRailImagePresentation(
                    image.id(), image.displayName(), image.relativePath(), assetUri(projectDirectory, image).orElse(""),
                    segmentId, segment.flatMap(blockFinder).orElse(""), segment.map(s -> s.preview(92)).orElse(""),
                    bindings.size(), bindings.stream().map(StoryboardBinding::segmentId).toList()));
        }
        return List.copyOf(result);
    }

    private static Optional<String> firstSourceBlockId(NarrationSegment segment) {
        return segment.sourceBlockIds().stream().filter(id -> id != null && !id.isBlank()).findFirst();
    }

    private static ScriptTextRange scriptRange(NarrationSegment segment, DocumentTextRange range, String preview) {
        int start = Math.min(range.startOffset(), segment.narrationText().length());
        int end = Math.min(Math.max(range.endOffset(), start), segment.narrationText().length());
        String text = preview == null ? "" : preview.strip();
        if (!text.isBlank()) {
            int index = segment.narrationText().indexOf(text);
            if (index >= 0) { start = index; end = index + text.length(); }
        }
        return new ScriptTextRange(segment.id(), start, end);
    }

    private static Optional<NarrativeLayerAssignment> imageAssignment(DocuPodcastProject project, ScriptTextRange range) {
        if (project == null || range == null) return Optional.empty();
        return project.narrativeLayerAssignments().stream()
                .filter(layer -> layer.kind() == NarrativeLayerKind.IMAGE)
                .filter(layer -> layer.textRange().segmentId().equals(range.segmentId()))
                .filter(layer -> layer.textRange().startOffset() < range.endOffset() && range.startOffset() < layer.textRange().endOffset())
                .findFirst();
    }

    private static Optional<ProjectAssetReference> imageAsset(DocuPodcastProject project, String assetId) {
        if (project == null || assetId == null || assetId.isBlank()) return Optional.empty();
        return project.assets().byId(assetId).filter(ProjectAssetReference::isImage);
    }

    private static StoryboardVisual storyboardVisual(DocuPodcastProject project,
                                                     StoryboardDocument storyboard,
                                                     String segmentId,
                                                     ProjectAssetReference fallbackImage,
                                                     Optional<Path> projectDirectory) {
        if (project == null) {
            return StoryboardVisual.empty(fallbackImage, projectDirectory);
        }
        StoryboardBinding binding = storyboard == null ? null : storyboard.bindingForSegment(segmentId).orElse(null);
        if (binding == null) {
            return StoryboardVisual.empty(fallbackImage, projectDirectory);
        }
        String officialAssetId = firstNonBlank(binding.metadata().get(UpsertTheatreStoryboardFrameVariantUseCase.OFFICIAL_IMAGE_ASSET_ID),
                fallbackImage == null ? "" : fallbackImage.id());
        String drawnAssetId = binding.metadata().getOrDefault(UpsertTheatreStoryboardFrameVariantUseCase.DRAWN_FRAME_ASSET_ID, "");
        String generatedAssetId = binding.metadata().getOrDefault(UpsertTheatreStoryboardFrameVariantUseCase.GENERATED_IMAGE_ASSET_ID, "");
        String sceneryAssetId = binding.metadata().getOrDefault(UpsertTheatreStoryboardFrameVariantUseCase.SCENERY_IMAGE_ASSET_ID, "");
        String activeVariant = binding.metadata().getOrDefault(UpsertTheatreStoryboardFrameVariantUseCase.ACTIVE_VISUAL_VARIANT,
                UpsertTheatreStoryboardFrameVariantUseCase.VARIANT_OFFICIAL);
        String activeAssetId = binding.imageAssetId();
        ProjectAssetReference active = imageAsset(project, activeAssetId).orElse(null);
        String activeUri = active == null ? "" : assetUri(projectDirectory, active).orElse("");
        String officialUri = imageAsset(project, officialAssetId).flatMap(asset -> assetUri(projectDirectory, asset)).orElse("");
        String drawnUri = imageAsset(project, drawnAssetId).flatMap(asset -> assetUri(projectDirectory, asset)).orElse("");
        String generatedUri = imageAsset(project, generatedAssetId).flatMap(asset -> assetUri(projectDirectory, asset)).orElse("");
        String sceneryUri = imageAsset(project, sceneryAssetId).flatMap(asset -> assetUri(projectDirectory, asset)).orElse("");
        if (active == null && fallbackImage != null) {
            activeAssetId = fallbackImage.id();
            activeUri = assetUri(projectDirectory, fallbackImage).orElse("");
        }
        return new StoryboardVisual(activeAssetId, activeUri, officialAssetId, officialUri,
                generatedAssetId, generatedUri, drawnAssetId, drawnUri,
                sceneryAssetId, sceneryUri, activeVariant);
    }

    private static Optional<String> assetUri(Optional<Path> projectDirectory, ProjectAssetReference asset) {
        if (projectDirectory.isEmpty() || asset == null) return Optional.empty();
        Path root = projectDirectory.get().toAbsolutePath().normalize();
        Path resolved = root.resolve(asset.relativePath()).normalize();
        if (!resolved.startsWith(root)) return Optional.empty();
        return Optional.of(resolved.toUri().toString());
    }

    private static String firstNonBlank(String first, String second) {
        String a = first == null ? "" : first.strip();
        return a.isBlank() ? (second == null ? "" : second.strip()) : a;
    }

    private record StoryboardVisual(String activeAssetId,
                                    String activeUri,
                                    String officialAssetId,
                                    String officialUri,
                                    String generatedAssetId,
                                    String generatedUri,
                                    String drawnFrameAssetId,
                                    String drawnFrameUri,
                                    String sceneryAssetId,
                                    String sceneryUri,
                                    String activeVariant) {
        private static StoryboardVisual empty(ProjectAssetReference image, Optional<Path> projectDirectory) {
            String assetId = image == null ? "" : image.id();
            String uri = image == null ? "" : assetUri(projectDirectory, image).orElse("");
            return new StoryboardVisual(assetId, uri, assetId, uri, "", "", "", "", "", "",
                    UpsertTheatreStoryboardFrameVariantUseCase.VARIANT_OFFICIAL);
        }
    }
}
