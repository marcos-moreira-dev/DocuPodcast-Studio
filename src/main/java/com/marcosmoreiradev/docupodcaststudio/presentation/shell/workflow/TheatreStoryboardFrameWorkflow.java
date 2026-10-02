package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.WorkspaceApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.UpsertTheatreStoryboardFrameVariantUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.TheatreVisualVariant;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.TheatreDrawingVaultItem;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreCameraApplicationPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreCameraReferenceResolver;
import com.marcosmoreiradev.docupodcaststudio.application.video.TheatreSceneryComposition;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreFrameSketchContext;

import java.io.IOException;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Coordinates drawn storyboard frames without growing the shell view model. */
public final class TheatreStoryboardFrameWorkflow {
    public boolean canEdit(ProjectMode mode, Optional<ProjectSession> active, NarrationScriptDocument script, String selectedSegmentId) {
        return mode == ProjectMode.THEATRE_PRODUCTION
                && active.isPresent()
                && script != null
                && selectedSegmentId != null
                && !selectedSegmentId.isBlank()
                && script.segmentById(selectedSegmentId).isPresent();
    }

    public Optional<TheatreFrameSketchContext> context(ProjectSession session,
                                                       StoryboardDocument storyboard,
                                                       NarrationScriptDocument script,
                                                       String selectedSegmentId,
                                                       Optional<Path> projectDirectory) {
        if (session == null || script == null || selectedSegmentId == null || selectedSegmentId.isBlank()) return Optional.empty();
        Optional<NarrationSegment> segment = script.segmentById(selectedSegmentId);
        if (segment.isEmpty()) return Optional.empty();
        BindingSnapshot binding = BindingSnapshot.from(storyboard, segment.get().id());
        DocuPodcastProject project = session.project();
        String officialUri = project.assets().byId(binding.officialAssetId()).flatMap(asset -> assetUri(projectDirectory, asset)).orElse("");
        String drawnUri = project.assets().byId(binding.drawnAssetId()).flatMap(asset -> assetUri(projectDirectory, asset)).orElse("");
        String drawnStateUri = relativeUri(projectDirectory, binding.drawnStatePath()).orElse("");
        String interventionId = interventionIdForSegment(project.theatre(), segment.get()).orElse("");
        String sceneId = sceneIdForIntervention(project.theatre(), interventionId);
        boolean applyCamera = new TheatreCameraApplicationPolicy().appliesToSegment(storyboard, segment.get());
        Optional<TheatreCameraReferenceResolver.ResolvedCamera> camera = applyCamera
                ? new TheatreCameraReferenceResolver().resolve(project, interventionId, projectDirectory.orElse(null))
                : Optional.empty();
        return Optional.of(new TheatreFrameSketchContext(
                segment.get().id(),
                interventionId,
                segment.get().title().isBlank() ? segment.get().id() : segment.get().title(),
                segment.get().narrationText(),
                officialUri,
                drawnUri,
                drawnStateUri,
                binding.activeVariant(),
                camera.map(resolved -> resolved.absolutePath().toUri().toString()).orElse(""),
                camera.map(resolved -> resolved.reference().displayName()).orElse(""),
                sceneObjectPreviews(project, projectDirectory, sceneId),
                loadDrawingVault(session)));
    }

    public FrameResult save(WorkspaceApplicationServices services,
                            ProjectSession session,
                            StoryboardDocument storyboard,
                            NarrationScriptDocument script,
                            String segmentId,
                            Path framePng,
                            String inkStateJson,
                            boolean activateDrawn,
                            List<TheatreDrawingVaultItem> drawingVault,
                            Optional<Path> projectDirectory) throws IOException {
        Path projectFile = session.projectFile().orElseThrow(() -> new IOException("Guarda el proyecto antes de guardar frames dibujados."));
        var result = services.generation().storyboard().upsertTheatreStoryboardFrameVariant().upsertDrawnFrame(
                session.project(), projectFile, storyboard, script, segmentId, framePng, inkStateJson,
                activateDrawn, drawingVault);
        session.replaceProject(result.project(), true);
        session.setStoryboard(result.storyboard());
        return FrameResult.from(segmentId, result.project(), result.storyboard(), result.activeAsset(), result.activeVariant(), projectDirectory,
                "Frame dibujado guardado para " + segmentId + ". Variante activa: " + result.activeVariant() + ".");
    }

    private static List<TheatreDrawingVaultItem> loadDrawingVault(ProjectSession session) {
        if (session == null || session.projectFile().isEmpty()) return List.of();
        try {
            return new UpsertTheatreStoryboardFrameVariantUseCase().loadDrawingVault(session.projectFile().get());
        } catch (IOException ignored) {
            return List.of();
        }
    }

    public FrameResult toggle(WorkspaceApplicationServices services,
                              ProjectSession session,
                              StoryboardDocument storyboard,
                              NarrationScriptDocument script,
                              String segmentId,
                              Optional<Path> projectDirectory) throws IOException {
        var binding = storyboard == null ? null : storyboard.bindingForSegment(segmentId).orElse(null);
        if (binding == null) throw new IOException("Dibuja o asigna una imagen antes de alternar variantes.");
        String currentVariant = binding.metadata().getOrDefault(UpsertTheatreStoryboardFrameVariantUseCase.ACTIVE_VISUAL_VARIANT,
                UpsertTheatreStoryboardFrameVariantUseCase.VARIANT_OFFICIAL);
        List<TheatreVisualVariant> available = List.of(TheatreVisualVariant.OFFICIAL,
                        TheatreVisualVariant.GENERATED, TheatreVisualVariant.DRAWN,
                        TheatreVisualVariant.SCENERY).stream()
                .filter(variant -> hasAsset(session.project(), binding.metadata(), variant))
                .toList();
        if (available.size() < 2) throw new IOException("No hay otra variante visual disponible para alternar.");
        TheatreVisualVariant current = TheatreVisualVariant.fromMetadata(currentVariant);
        int currentIndex = available.indexOf(current);
        String nextVariant = available.get((currentIndex < 0 ? 0 : currentIndex + 1) % available.size()).metadataValue();
        var result = services.generation().storyboard().upsertTheatreStoryboardFrameVariant().activateVariant(
                session.project(), storyboard, script, segmentId, nextVariant);
        session.replaceProject(result.project(), true);
        session.setStoryboard(result.storyboard());
        return FrameResult.from(segmentId, result.project(), result.storyboard(), result.activeAsset(), result.activeVariant(), projectDirectory,
                "Variante visual activa para " + segmentId + ": " + result.activeVariant() + ".");
    }

    public FrameResult activate(WorkspaceApplicationServices services,
                                ProjectSession session,
                                StoryboardDocument storyboard,
                                NarrationScriptDocument script,
                                String segmentId,
                                String variant,
                                Optional<Path> projectDirectory) throws IOException {
        var result = services.generation().storyboard().upsertTheatreStoryboardFrameVariant().activateVariant(
                session.project(), storyboard, script, segmentId, variant);
        session.replaceProject(result.project(), true);
        session.setStoryboard(result.storyboard());
        return FrameResult.from(segmentId, result.project(), result.storyboard(), result.activeAsset(), result.activeVariant(), projectDirectory,
                "Variante visual activa para " + segmentId + ": " + result.activeVariant() + ".");
    }

    public FrameResult materializeScenery(WorkspaceApplicationServices services,
                                          ProjectSession session,
                                          StoryboardDocument storyboard,
                                          NarrationScriptDocument script,
                                          String segmentId,
                                          Optional<Path> projectDirectory) throws IOException {
        Path projectFile = session.projectFile()
                .orElseThrow(() -> new IOException("Guarda el proyecto antes de crear la variante de escenografía."));
        Path root = projectDirectory.orElseGet(() -> projectFile.toAbsolutePath().normalize().getParent());
        NarrationSegment segment = script.segmentById(segmentId)
                .orElseThrow(() -> new IOException("No existe el segmento de storyboard: " + segmentId));
        String interventionId = interventionIdForSegment(session.project().theatre(), segment)
                .orElseThrow(() -> new IOException("El segmento no está asociado a una intervención teatral."));
        TheatreProjectLayer.TextActionPlacement placement = session.project().theatre().textActionPlacements().stream()
                .filter(value -> value.intervencionId().equals(interventionId))
                .findFirst()
                .orElseThrow(() -> new IOException("La intervención no tiene posiciones teatrales configuradas."));

        TheatreSceneryComposition composer = new TheatreSceneryComposition();
        var composition = composer.resolve(session.project(), placement, root);
        Path temporary = Files.createTempFile("docupodcast-scenery-", ".png");
        try {
            ImageIO.write(composer.render(composition, 1600, 900), "png", temporary.toFile());
            var result = services.generation().storyboard().upsertTheatreStoryboardFrameVariant()
                    .upsertSceneryFrame(session.project(), projectFile, storyboard, script,
                            segmentId, temporary, true);
            session.replaceProject(result.project(), true);
            session.setStoryboard(result.storyboard());
            return FrameResult.from(segmentId, result.project(), result.storyboard(), result.activeAsset(),
                    result.activeVariant(), projectDirectory,
                    "Variante Personajes y escenografía creada y activada para " + segmentId + ".");
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static boolean hasAsset(DocuPodcastProject project, Map<String, String> metadata,
                                    TheatreVisualVariant variant) {
        String key = switch (variant) {
            case OFFICIAL -> UpsertTheatreStoryboardFrameVariantUseCase.OFFICIAL_IMAGE_ASSET_ID;
            case GENERATED -> UpsertTheatreStoryboardFrameVariantUseCase.GENERATED_IMAGE_ASSET_ID;
            case DRAWN -> UpsertTheatreStoryboardFrameVariantUseCase.DRAWN_FRAME_ASSET_ID;
            case SCENERY -> UpsertTheatreStoryboardFrameVariantUseCase.SCENERY_IMAGE_ASSET_ID;
        };
        return project.assets().byId(metadata.getOrDefault(key, "")).filter(ProjectAssetReference::isImage).isPresent();
    }

    private static Optional<String> interventionIdForSegment(TheatreProjectLayer theatre, NarrationSegment segment) {
        if (theatre == null || segment == null || segment.sourceBlockIds().isEmpty()) return Optional.empty();
        return theatre.intervenciones().stream()
                .filter(intervention -> segment.sourceBlockIds().contains(intervention.blockId()))
                .map(TheatreProjectLayer.Intervencion::id)
                .findFirst();
    }

    private static String sceneIdForIntervention(TheatreProjectLayer theatre, String interventionId) {
        if (theatre == null || interventionId == null || interventionId.isBlank()) return "";
        return theatre.textActionPlacements().stream()
                .filter(placement -> placement.intervencionId().equals(interventionId))
                .map(TheatreProjectLayer.TextActionPlacement::sceneId)
                .filter(scene -> scene != null && !scene.isBlank())
                .findFirst()
                .orElse("");
    }

    private static List<TheatreFrameSketchContext.ObjectPreview> sceneObjectPreviews(
            DocuPodcastProject project,
            Optional<Path> projectDirectory,
            String sceneId) {
        TheatreProjectLayer theatre = project == null ? null : project.theatre();
        if (theatre == null || theatre.objects().isEmpty()) return List.of();
        List<TheatreProjectLayer.ObjectImage> sceneImages = sceneId == null || sceneId.isBlank()
                ? List.of()
                : theatre.objectImages().stream()
                .filter(image -> image.sceneId().equals(sceneId))
                .toList();
        if (sceneImages.isEmpty()) return List.of();
        Map<String, TheatreProjectLayer.TheatreObject> objectById = new LinkedHashMap<>();
        for (TheatreProjectLayer.TheatreObject object : theatre.objects()) {
            objectById.put(object.id(), object);
        }
        LinkedHashSet<String> seenObjects = new LinkedHashSet<>();
        ArrayList<TheatreFrameSketchContext.ObjectPreview> previews = new ArrayList<>();
        for (TheatreProjectLayer.ObjectImage image : sceneImages) {
            if (!seenObjects.add(image.objectId())) continue;
            TheatreProjectLayer.TheatreObject object = objectById.get(image.objectId());
            if (object == null) continue;
            String imageUri = project.assets().byId(image.assetId())
                    .flatMap(asset -> assetUri(projectDirectory, asset))
                    .orElse("");
            if (imageUri.isBlank()) continue;
            previews.add(new TheatreFrameSketchContext.ObjectPreview(
                    object.id(),
                    object.displayName(),
                    object.notes(),
                    imageUri));
            if (previews.size() >= 24) break;
        }
        return List.copyOf(previews);
    }

    private static Optional<String> assetUri(Optional<Path> projectDirectory, ProjectAssetReference asset) {
        if (projectDirectory.isEmpty() || asset == null) return Optional.empty();
        Path root = projectDirectory.get().toAbsolutePath().normalize();
        Path resolved = root.resolve(asset.relativePath()).normalize();
        if (!resolved.startsWith(root)) return Optional.empty();
        return Optional.of(resolved.toUri().toString());
    }

    private static Optional<String> relativeUri(Optional<Path> projectDirectory, String relativePath) {
        if (projectDirectory.isEmpty() || relativePath == null || relativePath.isBlank()) return Optional.empty();
        Path root = projectDirectory.get().toAbsolutePath().normalize();
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) return Optional.empty();
        return Optional.of(resolved.toUri().toString());
    }

    private record BindingSnapshot(String officialAssetId, String drawnAssetId, String drawnStatePath, String activeVariant) {
        private static BindingSnapshot from(StoryboardDocument storyboard, String segmentId) {
            var binding = storyboard == null ? null : storyboard.bindingForSegment(segmentId).orElse(null);
            if (binding == null) return new BindingSnapshot("", "", "", UpsertTheatreStoryboardFrameVariantUseCase.VARIANT_OFFICIAL);
            String active = binding.metadata().getOrDefault(UpsertTheatreStoryboardFrameVariantUseCase.ACTIVE_VISUAL_VARIANT,
                    UpsertTheatreStoryboardFrameVariantUseCase.VARIANT_OFFICIAL);
            String official = binding.metadata().getOrDefault(UpsertTheatreStoryboardFrameVariantUseCase.OFFICIAL_IMAGE_ASSET_ID, "");
            if (official.isBlank() && UpsertTheatreStoryboardFrameVariantUseCase.VARIANT_OFFICIAL.equals(active)) official = binding.imageAssetId();
            return new BindingSnapshot(
                    official,
                    binding.metadata().getOrDefault(UpsertTheatreStoryboardFrameVariantUseCase.DRAWN_FRAME_ASSET_ID, ""),
                    binding.metadata().getOrDefault(UpsertTheatreStoryboardFrameVariantUseCase.DRAWN_FRAME_STATE_PATH, ""),
                    active);
        }
    }

    public record FrameResult(String segmentId, DocuPodcastProject project, StoryboardDocument storyboard,
                              ProjectAssetReference activeAsset, String activeVariant,
                              String activeUri, String message) {
        private static FrameResult from(String segmentId, DocuPodcastProject project, StoryboardDocument storyboard,
                                        ProjectAssetReference activeAsset, String activeVariant,
                                        Optional<Path> projectDirectory, String message) {
            return new FrameResult(segmentId == null ? "" : segmentId.strip(), project, storyboard, activeAsset, activeVariant,
                    assetUri(projectDirectory, activeAsset).orElse(""), message);
        }
    }
}
