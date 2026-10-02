package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.video.TheatreSceneryComposition;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;
import java.io.ByteArrayInputStream;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/** Preview of the same alpha composition used for exported frames. */
final class TheatreSceneryPane extends StackPane {
    private static final ExecutorService RENDERER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = Thread.ofVirtual().unstarted(runnable);
        thread.setName("theatre-scenery-preview");
        return thread;
    });
    private final ImageView image = new ImageView();
    private final PauseTransition debounce = new PauseTransition(Duration.millis(90));
    private long revision;
    private RenderRequest pendingRequest;
    TheatreSceneryPane() {
        setMinSize(0,0); setPrefSize(960,540);
        setStyle("-fx-background-color: #000000;");
        image.setPreserveRatio(true);
        image.fitWidthProperty().bind(widthProperty());
        image.fitHeightProperty().bind(heightProperty());
        getChildren().add(image);
        debounce.setOnFinished(event -> renderPending());
    }
    void update(DocuPodcastShellViewModel vm, TheatreProjectLayer.TextActionPlacement placement) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> update(vm, placement));
            return;
        }
        boolean stageDirection = placement != null && IntervencionCatalogo.intervenciones(
                        vm.currentDocumentProperty().get(), vm.currentScriptProperty().get()).stream()
                .anyMatch(info -> info.stageDirection()
                        && placement.intervencionId().equals(info.alias()));
        long requested = ++revision;
        pendingRequest = new RenderRequest(requested, vm.currentProject().orElse(null), placement,
                vm.currentProjectDirectory().orElse(null), stageDirection);
        debounce.playFromStart();
    }

    private void renderPending() {
        RenderRequest request = pendingRequest;
        if (request == null) return;
        RENDERER.submit(() -> {
            var composer = new TheatreSceneryComposition();
            var composition = composer.resolve(request.project(), request.placement(), request.projectRoot());
            if (request.stageDirection()) {
                composition = new TheatreSceneryComposition.Composition(composition.backdrop(), java.util.List.of());
            }
            var renderedComposition = composition;
            Image rendered = null;
            try {
                rendered = new Image(new ByteArrayInputStream(composer.renderPng(renderedComposition, 1280, 720)));
            } catch(java.io.IOException | RuntimeException ignored) { }
            Image completed = rendered;
            String accessibility = "Escenografía. " + renderedComposition.figures().stream()
                    .map(f -> (f.speaker()?"Habla: ":"Se dirige a: ")+f.name())
                    .collect(java.util.stream.Collectors.joining(". "));
            Platform.runLater(() -> {
                if(revision != request.revision()) return;
                image.setAccessibleText(accessibility);
                image.setImage(completed);
            });
        });
    }

    private record RenderRequest(long revision,
                                 com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject project,
                                 TheatreProjectLayer.TextActionPlacement placement,
                                 java.nio.file.Path projectRoot,
                                 boolean stageDirection) { }
}
