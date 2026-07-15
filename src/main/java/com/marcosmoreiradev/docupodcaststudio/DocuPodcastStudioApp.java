package com.marcosmoreiradev.docupodcaststudio;

import com.marcosmoreiradev.docupodcaststudio.bootstrap.ApplicationBootstrap;
import com.marcosmoreiradev.docupodcaststudio.bootstrap.ApplicationRuntime;
import com.marcosmoreiradev.docupodcaststudio.bootstrap.ApplicationWindowConfig;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Objects;

/**
 * Entry point of DocuPodcast Studio.
 *
 * <p>The class is intentionally thin: it delegates application composition to
 * {@link ApplicationBootstrap}. This follows the desktop scaffolding approach
 * chosen during the onboarding phase.</p>
 */
public final class DocuPodcastStudioApp extends Application {

    @Override
    public void start(Stage stage) {
        ApplicationRuntime runtime = ApplicationBootstrap.createDefault().bootstrap();
        stage.initStyle(StageStyle.DECORATED);
        stage.setResizable(true);
        stage.setMaximized(false);
        ApplicationWindowConfig config = runtime.windowConfig();

        Scene scene = new Scene(runtime.root(), config.defaultWidth(), config.defaultHeight());
        runtime.stylesheetResources().forEach(resource -> {
            String resolved = DocuPodcastStudioApp.class.getResource(resource).toExternalForm();
            scene.getStylesheets().add(resolved);
        });

        stage.titleProperty().bind(runtime.windowTitleProperty());
        loadStageIcons(stage);
        stage.setScene(scene);
        stage.setMinWidth(config.minimumWidth());
        stage.setMinHeight(config.minimumHeight());
        fitStageInsideVisibleScreen(stage, config);
        stage.setOnCloseRequest(runtime.closeRequestHandler());
        stage.show();
        stage.centerOnScreen();
        if (runtime.root() instanceof DocuPodcastShellView shellView) {
            Platform.runLater(shellView::runStartupDependencyPreflight);
        }
    }

    private void loadStageIcons(Stage stage) {
        stage.getIcons().add(new Image(Objects.requireNonNull(
                DocuPodcastStudioApp.class.getResource("/branding/docupodcast-icon-32.png"),
                "Missing product icon 32px")
                .toExternalForm()));
        stage.getIcons().add(new Image(Objects.requireNonNull(
                DocuPodcastStudioApp.class.getResource("/branding/docupodcast-icon-64.png"),
                "Missing product icon 64px")
                .toExternalForm()));
        stage.getIcons().add(new Image(Objects.requireNonNull(
                DocuPodcastStudioApp.class.getResource("/branding/docupodcast-icon-256.png"),
                "Missing product icon 256px")
                .toExternalForm()));
    }

    private void fitStageInsideVisibleScreen(Stage stage, ApplicationWindowConfig config) {
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        double safeWidth = Math.max(config.minimumWidth(), visualBounds.getWidth() * 0.92);
        double safeHeight = Math.max(config.minimumHeight(), visualBounds.getHeight() * 0.88);
        stage.setWidth(Math.min(config.defaultWidth(), safeWidth));
        stage.setHeight(Math.min(config.defaultHeight(), safeHeight));
        stage.setX(visualBounds.getMinX() + Math.max(0, (visualBounds.getWidth() - stage.getWidth()) / 2));
        stage.setY(visualBounds.getMinY() + Math.max(0, (visualBounds.getHeight() - stage.getHeight()) / 2));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
