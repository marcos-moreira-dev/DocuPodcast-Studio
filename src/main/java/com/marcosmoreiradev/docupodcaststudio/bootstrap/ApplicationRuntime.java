package com.marcosmoreiradev.docupodcaststudio.bootstrap;

import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.stage.WindowEvent;

import java.util.List;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;

/** Runtime object returned by the bootstrap process. */
public record ApplicationRuntime(
        Parent root,
        ObservableValue<String> windowTitleProperty,
        ApplicationWindowConfig windowConfig,
        List<String> stylesheetResources,
        EventHandler<WindowEvent> closeRequestHandler,
        MediaEnginePlatform mediaEngines
) {
    public ApplicationRuntime {
        mediaEngines = mediaEngines == null ? MediaEnginePlatform.empty() : mediaEngines;
    }
}
