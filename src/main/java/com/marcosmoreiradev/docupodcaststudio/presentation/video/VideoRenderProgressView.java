package com.marcosmoreiradev.docupodcaststudio.presentation.video;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFeedbackControls;

import com.marcosmoreiradev.docupodcaststudio.application.video.VideoRenderProgress;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Blocking progress surface shown while the selected render engine exports video. */
public final class VideoRenderProgressView extends VBox {
    private final Label title = new Label("Renderizando video");
    private final Label stage = new Label();
    private final Label detail = new Label();
    private final Label elapsed = new Label();
    private final ProgressBar progress = StudioFeedbackControls.progressBar(0);
    private final Button cancelButton = ActionButtonFactory.danger("Cancelar render");
    private final Timeline elapsedTicker = new Timeline(
            new KeyFrame(Duration.seconds(1), event -> refreshElapsed()));
    private long startedAtNanos;

    public VideoRenderProgressView() {
        getStyleClass().add("video-render-progress");
        title.getStyleClass().add("video-render-title");
        stage.getStyleClass().add("video-render-stage");
        detail.getStyleClass().add("video-render-detail");
        elapsed.getStyleClass().add("video-render-detail");
        progress.getStyleClass().add("video-render-bar");
        progress.setMaxWidth(Double.MAX_VALUE);
        setPadding(new Insets(24));
        setSpacing(12);
        elapsedTicker.setCycleCount(Timeline.INDEFINITE);
        getChildren().addAll(title, stage, progress, detail, elapsed, cancelButton);
        update(VideoRenderProgress.preparing(0));
    }

    public void update(VideoRenderProgress state) {
        VideoRenderProgress safe = state == null ? VideoRenderProgress.idle() : state;
        String count = safe.totalFrames() <= 0 ? "" : " - " + safe.completedFrames() + "/" + safe.totalFrames() + " unidades";
        stage.setText(safe.stage().label() + count);
        detail.setText(safe.currentStep());
        progress.setProgress(safe.determinateProgress() ? safe.ratio() : -1.0);
        cancelButton.setDisable(!safe.cancellable());
        if (safe.cancellable()) {
            if (startedAtNanos == 0L) {
                startedAtNanos = System.nanoTime();
                elapsedTicker.play();
            }
        } else {
            elapsedTicker.stop();
        }
        refreshElapsed();
        setDisable(false);
    }

    private void refreshElapsed() {
        if (startedAtNanos == 0L) {
            elapsed.setText("");
            return;
        }
        long seconds = Math.max(0L, (System.nanoTime() - startedAtNanos) / 1_000_000_000L);
        long hours = seconds / 3600;
        long minutes = seconds % 3600 / 60;
        long remainingSeconds = seconds % 60;
        elapsed.setText(hours > 0
                ? "Tiempo transcurrido: %d h %02d min %02d s".formatted(hours, minutes, remainingSeconds)
                : "Tiempo transcurrido: %02d min %02d s".formatted(minutes, remainingSeconds));
    }

    public Button cancelButton() {
        return cancelButton;
    }
}
