package com.marcosmoreiradev.docupodcaststudio.presentation.narrative;

import com.marcosmoreiradev.docupodcaststudio.application.fragment.FragmentWorkspaceProjection;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualFragmentState;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualProductionProjection;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualSlotState;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ImageFullscreenViewer;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.MediaThumbnailCard;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** Operational visual production surface for Video narrativo over FragmentId. */
public final class NarrativeVisualProductionWorkspaceView extends BorderPane {
    private final DocuPodcastShellViewModel viewModel;
    private final VBox content = new VBox(12);
    private final BooleanProperty generationRunning = new SimpleBooleanProperty(false);

    public NarrativeVisualProductionWorkspaceView(DocuPodcastShellViewModel viewModel) {
        this.viewModel = viewModel;
        getStyleClass().add("narrative-visual-production-workspace");
        setPadding(new Insets(16));
        setTop(header());
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("narrative-visual-scroll");
        setCenter(scroll);
        viewModel.currentDocumentProperty().addListener((obs, oldValue, newValue) -> refresh());
        viewModel.currentScriptProperty().addListener((obs, oldValue, newValue) -> refresh());
        viewModel.currentStoryboardProperty().addListener((obs, oldValue, newValue) -> refresh());
        viewModel.currentPlaybackManifestProperty().addListener((obs, oldValue, newValue) -> refresh());
        viewModel.documentMediaRevisionProperty().addListener((obs, oldValue, newValue) -> refresh());
        refresh();
    }

    private HBox header() {
        Label title = new Label("Video narrativo");
        title.getStyleClass().add("workspace-title");
        Button refresh = readable(ActionButtonFactory.secondary("Actualizar", this::refresh), 112);
        HBox box = new HBox(12, title, spacer(), refresh);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(0, 0, 12, 0));
        return box;
    }

    private void refresh() {
        content.getChildren().clear();
        VisualProductionProjection projection = buildProjection();
        content.getChildren().add(summary(projection));
        if (projection.fragments().isEmpty()) {
            Label empty = new Label("Prepara la lectura o importa la gramatica narrativa para ver fragmentos visuales.");
            empty.getStyleClass().add("workspace-subtitle");
            content.getChildren().add(empty);
            return;
        }
        projection.fragments().forEach(fragment -> content.getChildren().add(card(fragment)));
        if (!projection.globalVisuals().isEmpty()) {
            Label global = new Label("Visuales heredados indexados: " + projection.globalVisuals().size());
            global.getStyleClass().add("workspace-subtitle");
            content.getChildren().add(global);
        }
    }

    private VisualProductionProjection buildProjection() {
        DocuPodcastProject project = viewModel.currentProject().orElse(null);
        Path projectDirectory = viewModel.currentProjectDirectory().orElse(null);
        FragmentWorkspaceProjection fragments = viewModel.applicationServices().fragment().buildFragmentWorkspaceProjection().build(
                viewModel.currentDocumentProperty().get(),
                viewModel.currentScriptProperty().get(),
                project,
                viewModel.currentStoryboardProperty().get(),
                List.of(),
                currentPlaybackManifest());
        return viewModel.applicationServices().visual().buildVisualProductionProjection().build(fragments, project, projectDirectory);
    }

    private VBox summary(VisualProductionProjection projection) {
        VBox box = new VBox(8);
        box.getStyleClass().add("narrative-visual-summary");
        Label title = new Label("Resumen operativo");
        title.getStyleClass().add("workspace-section-title");
        PlaybackManifest manifest = currentPlaybackManifest();
        long audioReady = projection.fragments().stream()
                .filter(fragment -> manifest.cueForSegment(fragment.segmentId()).isPresent())
                .count();
        FlowPane chips = new FlowPane(8, 8,
                metric("Fragmentos narrables", projection.readiness().narratableFragments() + "/" + projection.readiness().totalFragments()),
                metric("Imagen principal", projection.readiness().mainImageReadyCount() + "/" + projection.readiness().narratableFragments()),
                metric("Audio listo", audioReady + "/" + projection.readiness().narratableFragments()),
                metric("Puentes opcionales", String.valueOf(projection.readiness().bridgeImageReadyCount())),
                metric("Bloqueos", String.valueOf(projection.readiness().blockers().size())));
        box.getChildren().addAll(title, chips);
        if (!projection.readiness().blockers().isEmpty()) {
            Label blockers = new Label(String.join("  |  ", projection.readiness().blockers()));
            blockers.setWrapText(true);
            blockers.getStyleClass().add("status-error");
            box.getChildren().add(blockers);
        }
        return box;
    }

    private VBox card(VisualFragmentState fragment) {
        VBox card = new VBox(10);
        card.getStyleClass().add("document-visual-card");
        card.getStyleClass().add("narrative-fragment-card");
        card.setPadding(new Insets(12));
        Label title = new Label((fragment.order() + 1) + ". " + fragment.title());
        title.getStyleClass().add("document-visual-card-title");
        Label text = new Label(clip(fragment.text(), 240));
        text.setWrapText(true);
        text.getStyleClass().add("narrative-fragment-copy");

        HBox media = new HBox(10,
                thumbnail("Principal", fragment, fragment.mainImage()),
                thumbnail("Puente", fragment, fragment.bridgeImage()));
        media.setFillHeight(true);
        media.setMinWidth(0);
        HBox.setHgrow(media.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(media.getChildren().get(1), Priority.ALWAYS);

        FlowPane states = new FlowPane(8, 8,
                metric("Imagen principal", slotStatus(fragment.mainImage())),
                metric("Puente", slotStatus(fragment.bridgeImage())),
                metric("Audio", audioStatus(fragment.segmentId())),
                metric("Soporte", fragment.documentSupport().size() + " documentales"));

        HBox actions = actions(fragment);
        card.getChildren().addAll(title, text, media, states, actions);
        fragment.diagnostics().forEach(diagnostic -> {
            Label issue = new Label((diagnostic.blocking() ? "Bloqueo: " : "Aviso: ") + diagnostic.message());
            issue.setWrapText(true);
            issue.getStyleClass().add(diagnostic.blocking() ? "status-error" : "status-warning");
            card.getChildren().add(issue);
        });
        return card;
    }

    private MediaThumbnailCard thumbnail(String label, VisualFragmentState fragment, VisualSlotState slot) {
        String uri = slotUri(slot);
        String state = slot.ready() ? "document-media-ready" : slot.assigned() ? "document-media-warning" : "";
        return new MediaThumbnailCard(
                uri,
                label,
                label,
                slotStatus(slot),
                slot.assigned() ? firstPresent(slot.assetId(), slot.assetPath()) : "Sin imagen asignada",
                state,
                () -> viewModel.selectNarrativeVisualFragment(fragment.segmentId()));
    }

    private HBox actions(VisualFragmentState fragment) {
        Button chooseMain = readable(ActionButtonFactory.primary("Elegir imagen",
                "Importar una imagen principal 16:9 para este fragmento.",
                () -> chooseImage(fragment, NarrativeLayerKind.IMAGE)), 126);
        Button generate = readable(ActionButtonFactory.secondary("Generar con IA",
                "Generar una imagen principal con el motor local configurado.",
                () -> generateImage(fragment)), 132);
        Button chooseBridge = readable(ActionButtonFactory.secondary("Elegir puente",
                "Importar una imagen puente opcional hacia el siguiente fragmento.",
                () -> chooseImage(fragment, NarrativeLayerKind.BRIDGE_IMAGE)), 126);
        Button removeMain = readable(ActionButtonFactory.danger("Quitar",
                "Quitar la imagen principal de este fragmento.",
                () -> removeImage(fragment, NarrativeLayerKind.IMAGE)), 96);
        Button fullscreen = readable(ActionButtonFactory.secondary("Pantalla completa",
                "Ver la imagen principal en pantalla completa.",
                () -> showFullscreen(fragment)), 152);
        generate.disableProperty().bind(generationRunning);
        removeMain.setDisable(!fragment.mainImage().assigned());
        fullscreen.setDisable(slotUri(fragment.mainImage()).isBlank());
        HBox actions = new HBox(8, chooseMain, generate, chooseBridge, removeMain, fullscreen);
        actions.getStyleClass().add("narrative-fragment-actions");
        actions.setAlignment(Pos.CENTER_LEFT);
        return actions;
    }

    private void chooseImage(VisualFragmentState fragment, NarrativeLayerKind kind) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(kind == NarrativeLayerKind.BRIDGE_IMAGE ? "Elegir imagen puente" : "Elegir imagen principal");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imagenes", "*.png", "*.jpg", "*.jpeg", "*.webp"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
        File file = chooser.showOpenDialog(ownerWindow());
        if (file == null) {
            return;
        }
        try {
            viewModel.importImageForSegment(fragment.segmentId(), file.toPath(), kind);
            refresh();
        } catch (IOException | RuntimeException ex) {
            viewModel.reportUserVisibleError("No se pudo importar la imagen: " + message(ex));
        }
    }

    private void generateImage(VisualFragmentState fragment) {
        if (generationRunning.get()) {
            return;
        }
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                viewModel.generateNarrativeImageForSegment(fragment.segmentId());
                return null;
            }
        };
        generationRunning.set(true);
        task.setOnSucceeded(event -> {
            generationRunning.set(false);
            refresh();
        });
        task.setOnFailed(event -> {
            generationRunning.set(false);
            viewModel.reportUserVisibleError("No se pudo generar la imagen IA narrativa: " + message(task.getException()));
            refresh();
        });
        Thread thread = new Thread(task, "narrative-image-generation-" + safe(fragment.segmentId()));
        thread.setDaemon(true);
        thread.start();
    }

    private void removeImage(VisualFragmentState fragment, NarrativeLayerKind kind) {
        try {
            viewModel.removeImageAssignmentForSegment(fragment.segmentId(), kind);
            refresh();
        } catch (RuntimeException ex) {
            viewModel.reportUserVisibleError("No se pudo quitar la imagen: " + message(ex));
        }
    }

    private void showFullscreen(VisualFragmentState fragment) {
        String uri = slotUri(fragment.mainImage());
        if (uri.isBlank()) {
            viewModel.reportUserVisibleError("Ese fragmento no tiene imagen principal para mostrar.");
            return;
        }
        ImageFullscreenViewer.show(uri, ownerWindow(), stylesheets(), "Imagen narrativa",
                "No se pudo mostrar la imagen narrativa", viewModel::reportUserVisibleError);
    }

    private Label metric(String label, String value) {
        Label chip = new Label(label + ": " + value);
        chip.getStyleClass().add("narrative-metric-chip");
        chip.setWrapText(true);
        return chip;
    }

    private String slotStatus(VisualSlotState slot) {
        if (slot == null || !slot.assigned()) {
            return "sin asignar";
        }
        if (slot.missingAsset()) {
            return "archivo faltante";
        }
        return slot.ready() ? "lista" : slot.status().toLowerCase(Locale.ROOT);
    }

    private String audioStatus(String segmentId) {
        return currentPlaybackManifest().cueForSegment(segmentId).isPresent() ? "listo" : "pendiente";
    }

    private String slotUri(VisualSlotState slot) {
        if (slot == null) {
            return "";
        }
        String fromAsset = viewModel.projectImageAssetUri(slot.assetId()).orElse("");
        if (!fromAsset.isBlank()) {
            return fromAsset;
        }
        String path = slot.assetPath();
        if (path == null || path.isBlank()) {
            return "";
        }
        return viewModel.currentProjectDirectory()
                .map(root -> root.toAbsolutePath().normalize())
                .map(root -> root.resolve(path).normalize())
                .filter(resolved -> viewModel.currentProjectDirectory()
                        .map(root -> resolved.startsWith(root.toAbsolutePath().normalize()))
                        .orElse(false))
                .filter(Files::isRegularFile)
                .map(Path::toUri)
                .map(Object::toString)
                .orElse("");
    }

    private PlaybackManifest currentPlaybackManifest() {
        PlaybackManifest manifest = viewModel.currentPlaybackManifestProperty().get();
        return manifest == null ? PlaybackManifest.empty() : manifest;
    }

    private List<String> stylesheets() {
        return getScene() == null ? List.of() : List.copyOf(getScene().getStylesheets());
    }

    private Window ownerWindow() {
        return getScene() == null ? null : getScene().getWindow();
    }

    private static Button readable(Button button, double minWidth) {
        button.setMinWidth(minWidth);
        button.setPrefWidth(Region.USE_COMPUTED_SIZE);
        button.setMaxWidth(Region.USE_COMPUTED_SIZE);
        return button;
    }

    private static Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private static String clip(String text, int maxCharacters) {
        String value = text == null ? "" : text.strip();
        if (value.length() <= maxCharacters) {
            return value;
        }
        return value.substring(0, Math.max(0, maxCharacters)).strip() + "...";
    }

    private static String firstPresent(String first, String fallback) {
        String normalized = first == null ? "" : first.strip();
        return normalized.isBlank() ? (fallback == null ? "" : fallback.strip()) : normalized;
    }

    private static String message(Throwable error) {
        if (error == null) {
            return "error desconocido";
        }
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }

    private static String safe(String value) {
        String normalized = value == null ? "" : value.strip().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("^-+|-+$", "")
                .replaceAll("-+", "-");
        return normalized.isBlank() ? "fragmento" : normalized;
    }
}
