package com.marcosmoreiradev.docupodcaststudio.presentation.storyboard;

import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionBar;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.MetricBadge;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/** Live storyboard workspace with visual scene cards, image status, audio cues and validation. */
public final class StoryboardWorkspaceView extends BorderPane {
    private final DocuPodcastShellViewModel viewModel;
    private final ListView<String> scenes = new ListView<>();
    private final ListView<String> images = new ListView<>();
    private final ListView<String> validation = new ListView<>();
    private final VBox sceneBoard = new VBox(12);

    public StoryboardWorkspaceView(DocuPodcastShellViewModel viewModel) {
        this.viewModel = viewModel;
        getStyleClass().add("storyboard-workspace");
        setPadding(new Insets(16));
        setTop(header());
        setCenter(content());
        refresh();
        viewModel.currentScriptProperty().addListener((obs, oldValue, newValue) -> refresh());
        viewModel.currentStoryboardProperty().addListener((obs, oldValue, newValue) -> refresh());
        viewModel.statusMessageProperty().addListener((obs, oldValue, newValue) -> refresh());
        viewModel.playbackCursorProperty().addListener((obs, oldValue, newValue) -> refresh());
        viewModel.currentPlaybackManifestProperty().addListener((obs, oldValue, newValue) -> refresh());
        viewModel.selectedScriptSegmentIdProperty().addListener((obs, oldValue, newValue) -> refresh());
    }

    private VBox header() {
        Label title = new Label("Secuencia visual interna");
        title.getStyleClass().add("workspace-title");
        Label subtitle = new Label("Organiza escenas visuales por segmento: imagen aportada por el usuario, estado de audio, validación y playback sincronizado.");
        subtitle.getStyleClass().add("workspace-subtitle");
        var build = ActionButtonFactory.primary("Crear desde documento", viewModel::buildStoryboardFromScript);
        var importImage = ActionButtonFactory.secondary("Importar imagen", () -> {
            if (getScene() != null && getScene().getRoot() instanceof com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellView shell) {
                shell.handleImportStoryboardImage();
            }
        });
        var bind = ActionButtonFactory.secondary("Asociar imagen al segmento seleccionado", viewModel::bindLastStoryboardImageToSelectedSegment);
        var play = ActionButtonFactory.secondary("Play desde selección", viewModel::playFromSelectedSegment);
        ActionBar actions = new ActionBar(build, importImage, bind, play);
        actions.getStyleClass().add("storyboard-action-row");
        Label sync = new Label();
        sync.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> viewModel.playbackSyncState().summaryLabel() + " · " + viewModel.playbackSyncState().readinessLabel(),
                viewModel.currentPlaybackManifestProperty(),
                viewModel.playbackCursorProperty(),
                viewModel.currentStoryboardProperty()));
        sync.setWrapText(true);
        sync.getStyleClass().add("storyboard-sync-summary");
        VBox box = new VBox(6, title, subtitle, actions, sync, new Separator());
        box.setPadding(new Insets(0, 0, 12, 0));
        return box;
    }

    private BorderPane content() {
        BorderPane pane = new BorderPane();
        pane.setLeft(side());
        pane.setCenter(preview());
        return pane;
    }

    private VBox side() {
        Label scenesTitle = new Label("Escenas / segmentos");
        scenesTitle.getStyleClass().add("panel-title");
        scenes.setPrefWidth(390);
        scenes.setMinHeight(220);
        Label imagesTitle = new Label("Imágenes importadas");
        imagesTitle.getStyleClass().add("panel-title");
        images.setPrefWidth(390);
        images.setMinHeight(140);
        Label validationTitle = new Label("Validación");
        validationTitle.getStyleClass().add("panel-title");
        validation.setPrefWidth(390);
        validation.setMinHeight(110);
        Label helpTitle = new Label("Uso operativo");
        helpTitle.getStyleClass().add("panel-title");
        Label help = new Label("Selecciona una escena para activar su segmento. Importa una imagen, asóciala al segmento y usa Play para revisar audio + visuales desde esa escena.");
        help.setWrapText(true);
        help.getStyleClass().add("document-side-text");
        Label syncTitle = new Label("Sincronización");
        syncTitle.getStyleClass().add("panel-title");
        Label sync = new Label();
        sync.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> viewModel.playbackSyncState().activeLabel(),
                viewModel.playbackCursorProperty(),
                viewModel.currentPlaybackManifestProperty()));
        sync.setWrapText(true);
        sync.getStyleClass().add("document-side-text");
        VBox side = new VBox(8, scenesTitle, scenes, imagesTitle, images, validationTitle, validation, syncTitle, sync, helpTitle, help);
        side.getStyleClass().add("workspace-side-panel");
        side.setPadding(new Insets(0, 14, 0, 0));
        return side;
    }

    private ScrollPane preview() {
        sceneBoard.getStyleClass().add("storyboard-scene-board");
        sceneBoard.setPadding(new Insets(4, 0, 0, 14));
        ScrollPane scroll = new ScrollPane(sceneBoard);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("storyboard-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    private void refresh() {
        scenes.getItems().setAll(viewModel.storyboardSceneLabels());
        images.getItems().setAll(viewModel.storyboardImageAssetLabels());
        validation.getItems().setAll(viewModel.storyboardValidationLabels());
        renderSceneBoard();
    }

    private void renderSceneBoard() {
        sceneBoard.getChildren().clear();
        List<StoryboardScenePresentation> cards = viewModel.storyboardScenePresentations();
        if (cards.isEmpty()) {
            sceneBoard.getChildren().add(emptyCard("Prepara la lectura del documento para construir la secuencia visual."));
            return;
        }
        sceneBoard.getChildren().add(summary(cards));
        for (StoryboardScenePresentation card : cards) {
            sceneBoard.getChildren().add(sceneCard(card));
        }
    }

    private HBox summary(List<StoryboardScenePresentation> cards) {
        long withImages = cards.stream().filter(StoryboardScenePresentation::imageReady).count();
        long withAudio = cards.stream().filter(StoryboardScenePresentation::audioReady).count();
        long valid = cards.stream().filter(StoryboardScenePresentation::validationOk).count();
        HBox summary = new HBox(8,
                metric("Escenas", String.valueOf(cards.size()), "segmentos visuales"),
                metric("Imágenes", withImages + "/" + cards.size(), "escenas con asset"),
                metric("Audio", withAudio + "/" + cards.size(), "escenas con cue"),
                metric("Validación", valid + "/" + cards.size(), "listas para revisión"));
        summary.getStyleClass().add("storyboard-summary-row");
        return summary;
    }

    private VBox metric(String title, String value, String caption) {
        VBox box = new MetricBadge(title, value, caption, "storyboard-summary-card");
        return box;
    }

    private HBox sceneCard(StoryboardScenePresentation scene) {
        HBox card = new HBox(12);
        card.getStyleClass().addAll("storyboard-scene-card", scene.cardStateCssClass());
        card.setAlignment(Pos.TOP_LEFT);
        card.setOnMouseClicked(event -> viewModel.selectScriptSegment(scene.segmentId()));
        StackPane thumbnail = thumbnail(scene);
        VBox body = new VBox(7);
        HBox heading = new HBox(8);
        heading.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(scene.segmentId() + " — " + scene.title());
        title.getStyleClass().add("storyboard-scene-title");
        HBox.setHgrow(title, Priority.ALWAYS);
        Label playback = new Label(playbackLabel(scene));
        playback.getStyleClass().add(playbackCss(scene));
        heading.getChildren().addAll(title, playback);

        FlowPane chips = new FlowPane(6, 6,
                chip(scene.imageStatus(), scene.imageCssClass()),
                chip(scene.audioStatus(), scene.audioCssClass()),
                chip(scene.validationStatus(), scene.validationCssClass()),
                chip(scene.displayModeLabel(), "storyboard-chip-neutral"));
        chips.getStyleClass().add("storyboard-chip-row");

        Label caption = new Label(scene.caption());
        caption.getStyleClass().add("storyboard-caption");
        caption.setWrapText(true);
        Label imagePath = new Label(scene.imageRelativePath().isBlank()
                ? "Asset visual: pendiente"
                : "Asset visual: " + scene.imageDisplayName() + " · " + scene.imageRelativePath());
        imagePath.getStyleClass().add("storyboard-asset-path");
        imagePath.setWrapText(true);
        Label text = new Label(scene.narrationPreview());
        text.getStyleClass().add("storyboard-narration-preview");
        text.setWrapText(true);

        var select = ActionButtonFactory.secondary("Seleccionar", () -> viewModel.selectScriptSegment(scene.segmentId()));
        var bind = ActionButtonFactory.secondary("Asociar última imagen", () -> {
            viewModel.selectScriptSegment(scene.segmentId());
            viewModel.bindLastStoryboardImageToSelectedSegment();
        });
        var play = ActionButtonFactory.primary("Play", () -> viewModel.playFromSegment(scene.segmentId()));
        ActionBar actions = new ActionBar(select, bind, play);

        body.getChildren().addAll(heading, chips, caption, imagePath, text, actions);
        HBox.setHgrow(body, Priority.ALWAYS);
        card.getChildren().addAll(thumbnail, body);
        return card;
    }

    private StackPane thumbnail(StoryboardScenePresentation scene) {
        StackPane thumbnail = new StackPane();
        thumbnail.getStyleClass().add(scene.imageReady() ? "storyboard-thumbnail" : "storyboard-thumbnail-empty");
        thumbnail.setMinSize(156, 96);
        thumbnail.setPrefSize(156, 96);
        if (scene.imageReady() && !scene.imageFileUri().isBlank()) {
            ImageView imageView = new ImageView(new Image(scene.imageFileUri(), 156, 96, true, true, true));
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(156);
            imageView.setFitHeight(96);
            thumbnail.getChildren().add(imageView);
        } else {
            VBox placeholder = new VBox(4);
            placeholder.setAlignment(Pos.CENTER);
            Label icon = new Label("▧");
            icon.getStyleClass().add("storyboard-thumbnail-icon");
            Label text = new Label(scene.imageReady() ? "Imagen sin preview" : "Sin imagen");
            text.getStyleClass().add("storyboard-thumbnail-text");
            placeholder.getChildren().addAll(icon, text);
            thumbnail.getChildren().add(placeholder);
        }
        return thumbnail;
    }

    private Label chip(String text, String cssClass) {
        Label label = new Label(text);
        label.getStyleClass().addAll("storyboard-status-chip", cssClass);
        return label;
    }

    private String playbackLabel(StoryboardScenePresentation scene) {
        if (scene.playbackActive()) {
            return "En reproducción";
        }
        if (scene.playbackPaused()) {
            return "Pausado";
        }
        if (scene.selected()) {
            return "Seleccionado";
        }
        return "Listo para revisar";
    }

    private String playbackCss(StoryboardScenePresentation scene) {
        if (scene.playbackActive()) {
            return "storyboard-playback-active";
        }
        if (scene.playbackPaused()) {
            return "storyboard-playback-paused";
        }
        if (scene.selected()) {
            return "storyboard-playback-selected";
        }
        return "storyboard-playback-idle";
    }

    private VBox emptyCard(String message) {
        VBox card = new VBox(new Label(message));
        card.getStyleClass().add("storyboard-empty-card");
        return card;
    }
}
