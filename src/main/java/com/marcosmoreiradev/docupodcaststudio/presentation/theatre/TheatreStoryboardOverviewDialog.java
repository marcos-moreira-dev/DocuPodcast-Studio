package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeSourceChooser;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioViewportControls;

import com.marcosmoreiradev.docupodcaststudio.application.storyboard.UpsertTheatreStoryboardFrameVariantUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ImageFullscreenViewer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Read-only visual history board for theatre projects. */
public final class TheatreStoryboardOverviewDialog {
    private TheatreStoryboardOverviewDialog() {
    }

    public static void show(DocuPodcastShellViewModel viewModel, Window owner, Collection<String> stylesheets) {
        if (viewModel == null) {
            return;
        }
        DocuPodcastProject project = viewModel.currentProject().orElse(null);
        NarrationScriptDocument script = viewModel.currentScriptProperty().get();
        if (project == null || script == null || script.empty()) {
            return;
        }

        Stage stage = new Stage();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle("History board");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("document-media-storyboard-overview");
        Label title = new Label("History board");
        title.getStyleClass().add("document-media-section-title");
        Label hint = new Label("Mapa visual de consulta por actos y escenas. Cada intervencion muestra la variante activa.");
        hint.setWrapText(true);
        HBox header = new HBox(14, title, hint);
        HBox.setHgrow(hint, Priority.ALWAYS);
        header.setPadding(new Insets(18, 22, 12, 22));
        header.setAlignment(Pos.CENTER_LEFT);
        root.setTop(header);

        VBox content = new VBox(18);
        content.setPadding(new Insets(8, 22, 24, 22));
        ScrollPane scroll = StudioViewportControls.scrollPane(content);
        scroll.setFitToWidth(true);
        root.setCenter(scroll);

        Runnable rebuild = () -> rebuildContent(content, viewModel, project, script, stage, stylesheets);
        rebuild.run();

        Scene scene = new Scene(root, 1220, 760);
        scene.getStylesheets().addAll(stylesheets == null ? List.of() : stylesheets);
        stage.setScene(scene);
        stage.show();
    }

    private static void rebuildContent(VBox content,
                                       DocuPodcastShellViewModel viewModel,
                                       DocuPodcastProject project,
                                       NarrationScriptDocument script,
                                       Window owner,
                                       Collection<String> stylesheets) {
        content.getChildren().clear();
        TheatreProjectLayer theatre = project.theatre();
        List<TheatreProjectLayer.TheatreAct> acts = theatre.acts().isEmpty()
                ? List.of(new TheatreProjectLayer.TheatreAct("_", "Obra", ""))
                : theatre.acts();
        List<TheatreProjectLayer.Scene> scenes = theatre.scenes();
        Map<String, String> sceneByIntervention = sceneByIntervention(theatre);
        Map<String, NarrationSegment> segmentByIntervention = segmentByIntervention(theatre, script);

        for (TheatreProjectLayer.TheatreAct act : acts) {
            VBox actBox = new VBox(10);
            actBox.getStyleClass().add("theatre-storyboard-overview-act");
            Label actTitle = new Label(act.displayName());
            actTitle.getStyleClass().add("document-media-frame-title");
            actBox.getChildren().add(actTitle);

            List<TheatreProjectLayer.Scene> actScenes = scenes.stream()
                    .filter(scene -> act.id().equals("_") || scene.actId().isBlank() || scene.actId().equals(act.id()))
                    .toList();
            if (actScenes.isEmpty() && act.id().equals("_")) {
                actScenes = List.of(new TheatreProjectLayer.Scene("_", "Storyboard", ""));
            }

            for (TheatreProjectLayer.Scene scene : actScenes) {
                VBox sceneBox = new VBox(8);
                sceneBox.getStyleClass().add("theatre-storyboard-overview-scene");
                Label sceneTitle = new Label(scene.displayName());
                sceneTitle.getStyleClass().add("document-media-counter-label");
                TheatreVisualSequenceMap map = new TheatreVisualSequenceMap(interventionItems(
                        content,
                        viewModel,
                        project,
                        script,
                        theatre,
                        sceneByIntervention,
                        segmentByIntervention,
                        scene.id(),
                        owner,
                        stylesheets));
                sceneBox.getChildren().addAll(sceneTitle, map);
                actBox.getChildren().add(sceneBox);
            }

            content.getChildren().add(actBox);
        }
    }

    private static Map<String, String> sceneByIntervention(TheatreProjectLayer theatre) {
        Map<String, String> sceneByIntervention = new LinkedHashMap<>();
        for (TheatreProjectLayer.TextActionPlacement placement : theatre.textActionPlacements()) {
            sceneByIntervention.put(placement.intervencionId(), placement.sceneId());
        }
        return sceneByIntervention;
    }

    private static List<TheatreVisualSequenceMap.Item> interventionItems(VBox content,
                                                                          DocuPodcastShellViewModel viewModel,
                                                                          DocuPodcastProject project,
                                                                          NarrationScriptDocument script,
                                                                          TheatreProjectLayer theatre,
                                                                          Map<String, String> sceneByIntervention,
                                                                          Map<String, NarrationSegment> segmentByIntervention,
                                                                          String sceneId,
                                                                          Window owner,
                                                                          Collection<String> stylesheets) {
        return theatre.intervenciones().stream()
                .filter(intervention -> sceneId.equals("_")
                        || sceneId.equals(sceneByIntervention.getOrDefault(intervention.id(), "")))
                .sorted(Comparator.comparingInt(TheatreProjectLayer.Intervencion::sequenceIndex))
                .map(intervention -> item(
                        content,
                        viewModel,
                        project,
                        script,
                        theatre,
                        segmentByIntervention.get(intervention.id()),
                        intervention,
                        owner,
                        stylesheets))
                .toList();
    }

    private static TheatreVisualSequenceMap.Item item(VBox content,
                                                       DocuPodcastShellViewModel viewModel,
                                                       DocuPodcastProject project,
                                                       NarrationScriptDocument script,
                                                       TheatreProjectLayer theatre,
                                                       NarrationSegment segment,
                                                       TheatreProjectLayer.Intervencion intervention,
                                                       Window owner,
                                                       Collection<String> stylesheets) {
        StoryboardDocument storyboard = viewModel.currentStoryboardProperty().get();
        VisualSnapshot visual = VisualSnapshot.from(project, viewModel.currentProjectDirectory(), storyboard, segment);
        String title = "Intervencion " + intervention.sequenceIndex();
        String preview = segment == null ? intervention.blockId() : segment.preview(116);
        return new TheatreVisualSequenceMap.Item(
                segment == null ? "" : segment.id(),
                title,
                preview,
                visual.imageUri(),
                visual.label(),
                visual.canToggle(),
                visual.canExport(),
                () -> showFullscreen(viewModel, owner, stylesheets, visual, title),
                () -> toggleVariant(content, viewModel, project, script, segment, owner, stylesheets),
                () -> exportFrame(viewModel, owner, visual, title));
    }

    private static void showFullscreen(DocuPodcastShellViewModel viewModel,
                                       Window owner,
                                       Collection<String> stylesheets,
                                       VisualSnapshot visual,
                                       String title) {
        if (visual == null || visual.imageUri().isBlank()) {
            viewModel.updateStatusMessage("Ese frame no tiene imagen para mostrar.");
            return;
        }
        ImageFullscreenViewer.show(
                visual.imageUri(),
                owner,
                stylesheets == null ? List.of() : stylesheets,
                title,
                "No se pudo mostrar el frame",
                viewModel::updateStatusMessage);
    }

    private static void toggleVariant(VBox content,
                                      DocuPodcastShellViewModel viewModel,
                                      DocuPodcastProject project,
                                      NarrationScriptDocument script,
                                      NarrationSegment segment,
                                      Window owner,
                                      Collection<String> stylesheets) {
        if (segment == null) {
            return;
        }
        viewModel.toggleTheatreStoryboardFrameVariant(segment.id());
        DocuPodcastProject refreshedProject = viewModel.currentProject().orElse(project);
        NarrationScriptDocument refreshedScript = viewModel.currentScriptProperty().get();
        rebuildContent(content, viewModel, refreshedProject, refreshedScript == null ? script : refreshedScript, owner, stylesheets);
    }

    private static void exportFrame(DocuPodcastShellViewModel viewModel,
                                    Window owner,
                                    VisualSnapshot visual,
                                    String title) {
        if (visual == null || visual.imagePath().isEmpty()) {
            viewModel.updateStatusMessage("Ese frame no tiene archivo exportable.");
            return;
        }
        Path source = visual.imagePath().get();
        if (!Files.isRegularFile(source)) {
            viewModel.updateStatusMessage("No se encontro el archivo del frame activo.");
            return;
        }

        String extension = fileExtension(source).orElse(".png");
        FileChooser chooser = NativeSourceChooser.fileChooser();
        chooser.setTitle("Exportar frame");
        chooser.setInitialFileName(safeFileName(title) + extension);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagen " + extension, "*" + extension));
        File target = chooser.showSaveDialog(owner);
        if (target == null) {
            return;
        }
        try {
            Files.copy(source, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            viewModel.updateStatusMessage("Frame exportado: " + target.getName() + ".");
        } catch (IOException ex) {
            viewModel.updateStatusMessage("No se pudo exportar el frame: " + ex.getMessage());
        }
    }

    private static Map<String, NarrationSegment> segmentByIntervention(TheatreProjectLayer theatre, NarrationScriptDocument script) {
        Map<String, NarrationSegment> byIntervention = new LinkedHashMap<>();
        for (TheatreProjectLayer.Intervencion intervention : theatre.intervenciones()) {
            script.segments().stream()
                    .filter(segment -> segment.sourceBlockIds().contains(intervention.blockId()))
                    .findFirst()
                    .ifPresent(segment -> byIntervention.put(intervention.id(), segment));
        }
        return byIntervention;
    }

    private static Optional<Path> assetPath(Optional<Path> projectDirectory, ProjectAssetReference asset) {
        if (projectDirectory.isEmpty() || asset == null) {
            return Optional.empty();
        }
        Path root = projectDirectory.get().toAbsolutePath().normalize();
        Path resolved = root.resolve(asset.relativePath()).normalize();
        if (!resolved.startsWith(root)) {
            return Optional.empty();
        }
        return Optional.of(resolved);
    }

    private static String assetUri(Optional<Path> projectDirectory, ProjectAssetReference asset) {
        return assetPath(projectDirectory, asset).map(path -> path.toUri().toString()).orElse("");
    }

    private static String safeFileName(String value) {
        String normalized = value == null ? "frame" : value.strip().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9._-]+", "-").replaceAll("^-+|-+$", "");
        return normalized.isBlank() ? "frame" : normalized;
    }

    private static Optional<String> fileExtension(Path path) {
        if (path == null || path.getFileName() == null) {
            return Optional.empty();
        }
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return Optional.empty();
        }
        return Optional.of(name.substring(dot).toLowerCase(Locale.ROOT));
    }

    private record VisualSnapshot(String imageUri, Optional<Path> imagePath, String label, boolean canToggle, boolean canExport) {
        private static VisualSnapshot from(DocuPodcastProject project,
                                           Optional<Path> projectDirectory,
                                           StoryboardDocument storyboard,
                                           NarrationSegment segment) {
            if (project == null || segment == null || storyboard == null) {
                return empty();
            }
            StoryboardBinding binding = storyboard.bindingForSegment(segment.id()).orElse(null);
            if (binding == null) {
                return empty();
            }
            Map<String, String> metadata = binding.metadata();
            String active = metadata.getOrDefault(UpsertTheatreStoryboardFrameVariantUseCase.ACTIVE_VISUAL_VARIANT,
                    UpsertTheatreStoryboardFrameVariantUseCase.VARIANT_OFFICIAL);
            String official = metadata.getOrDefault(UpsertTheatreStoryboardFrameVariantUseCase.OFFICIAL_IMAGE_ASSET_ID, "");
            String drawn = metadata.getOrDefault(UpsertTheatreStoryboardFrameVariantUseCase.DRAWN_FRAME_ASSET_ID, "");
            if (official.isBlank() && !UpsertTheatreStoryboardFrameVariantUseCase.VARIANT_DRAWN.equals(active)) {
                official = binding.imageAssetId();
            }
            String activeAsset = UpsertTheatreStoryboardFrameVariantUseCase.VARIANT_DRAWN.equals(active)
                    ? drawn
                    : firstNonBlank(official, binding.imageAssetId());
            Optional<ProjectAssetReference> asset = project.assets().byId(activeAsset);
            Optional<Path> path = asset.flatMap(reference -> assetPath(projectDirectory, reference));
            String uri = asset.map(reference -> assetUri(projectDirectory, reference)).orElse("");
            boolean drawnActive = UpsertTheatreStoryboardFrameVariantUseCase.VARIANT_DRAWN.equals(active);
            String label = drawnActive ? "Frame dibujado" : uri.isBlank() ? "Boceto pendiente" : "Imagen";
            boolean canToggle = !official.isBlank() && !drawn.isBlank()
                    && project.assets().byId(official).isPresent()
                    && project.assets().byId(drawn).isPresent();
            boolean canExport = path.filter(Files::isRegularFile).isPresent();
            return new VisualSnapshot(uri, path, label, canToggle, canExport);
        }

        private static VisualSnapshot empty() {
            return new VisualSnapshot("", Optional.empty(), "Boceto pendiente", false, false);
        }

        private static String firstNonBlank(String first, String second) {
            String safeFirst = first == null ? "" : first.strip();
            return safeFirst.isBlank() ? (second == null ? "" : second.strip()) : safeFirst;
        }
    }
}
