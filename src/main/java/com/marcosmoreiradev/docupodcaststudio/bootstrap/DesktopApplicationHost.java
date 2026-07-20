package com.marcosmoreiradev.docupodcaststudio.bootstrap;

import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;
import com.marcosmoreiradev.docupodcaststudio.ink.StudioInkPlatform;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellView;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Objects;

/** Desktop host invoked by the launcher after all platform adapters are composed. */
public final class DesktopApplicationHost {
    private final MediaEnginePlatform mediaEngines;
    private final StudioInkPlatform inkPlatform;

    public DesktopApplicationHost(MediaEnginePlatform mediaEngines, StudioInkPlatform inkPlatform) {
        this.mediaEngines = Objects.requireNonNull(mediaEngines, "media engines");
        this.inkPlatform = Objects.requireNonNull(inkPlatform, "ink platform");
    }

    public void start(Stage stage) {
        ApplicationRuntime runtime = ApplicationBootstrap.createDefault(mediaEngines, inkPlatform).bootstrap();
        stage.initStyle(StageStyle.DECORATED);
        stage.setResizable(true);
        stage.setMaximized(false);
        ApplicationWindowConfig config = runtime.windowConfig();

        Scene scene = new Scene(runtime.root(), config.defaultWidth(), config.defaultHeight());
        runtime.stylesheetResources().forEach(resource -> scene.getStylesheets().add(Objects.requireNonNull(
                DesktopApplicationHost.class.getResource(resource), "Missing stylesheet " + resource).toExternalForm()));
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

    private static void loadStageIcons(Stage stage) {
        for (int size : new int[]{32, 64, 256}) {
            stage.getIcons().add(new Image(Objects.requireNonNull(
                    DesktopApplicationHost.class.getResource("/branding/docupodcast-icon-" + size + ".png"),
                    "Missing product icon " + size + "px").toExternalForm()));
        }
    }

    private static void fitStageInsideVisibleScreen(Stage stage, ApplicationWindowConfig config) {
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        double safeWidth = Math.max(config.minimumWidth(), visualBounds.getWidth() * 0.92);
        double safeHeight = Math.max(config.minimumHeight(), visualBounds.getHeight() * 0.88);
        stage.setWidth(Math.min(config.defaultWidth(), safeWidth));
        stage.setHeight(Math.min(config.defaultHeight(), safeHeight));
        stage.setX(visualBounds.getMinX() + Math.max(0, (visualBounds.getWidth() - stage.getWidth()) / 2));
        stage.setY(visualBounds.getMinY() + Math.max(0, (visualBounds.getHeight() - stage.getHeight()) / 2));
    }
}
