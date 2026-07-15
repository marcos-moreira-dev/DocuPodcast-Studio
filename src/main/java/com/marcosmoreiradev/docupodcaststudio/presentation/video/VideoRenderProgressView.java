package com.marcosmoreiradev.docupodcaststudio.presentation.video;

import com.marcosmoreiradev.docupodcaststudio.application.video.VideoRenderProgress;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

/** Blocking render progress surface shown while FFmpeg exports the simple video. */
public final class VideoRenderProgressView extends VBox {
    private final Label title = new Label("Renderizando video");
    private final Label stage = new Label();
    private final Label detail = new Label();
    private final ProgressBar progress = new ProgressBar(0);
    private final Button cancelButton = new Button("Cancelar render");

    public VideoRenderProgressView() {
        getStyleClass().add("video-render-progress");
        title.getStyleClass().add("video-render-title");
        stage.getStyleClass().add("video-render-stage");
        detail.getStyleClass().add("video-render-detail");
        cancelButton.getStyleClass().addAll("ui-action-button", "ui-action-button-danger");
        progress.getStyleClass().add("video-render-bar");
        progress.setMaxWidth(Double.MAX_VALUE);
        setPadding(new Insets(24));
        setSpacing(12);
        getChildren().addAll(title, stage, progress, detail, cancelButton);
        update(VideoRenderProgress.preparing(0));
    }

    public void update(VideoRenderProgress state) {
        VideoRenderProgress safe = state == null ? VideoRenderProgress.idle() : state;
        String count = safe.totalFrames() <= 0 ? "" : " - " + safe.completedFrames() + "/" + safe.totalFrames() + " unidades";
        stage.setText(safe.stage().label() + count);
        detail.setText(safe.currentStep());
        progress.setProgress(safe.totalFrames() <= 0 && safe.cancellable() ? -1.0 : safe.ratio());
        cancelButton.setDisable(!safe.cancellable());
        setDisable(false);
    }

    public Button cancelButton() {
        return cancelButton;
    }
}
