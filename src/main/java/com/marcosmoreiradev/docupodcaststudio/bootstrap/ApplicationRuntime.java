package com.marcosmoreiradev.docupodcaststudio.bootstrap;

import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.stage.WindowEvent;

import java.util.List;

/** Runtime object returned by the bootstrap process. */
public record ApplicationRuntime(
        Parent root,
        ObservableValue<String> windowTitleProperty,
        ApplicationWindowConfig windowConfig,
        List<String> stylesheetResources,
        EventHandler<WindowEvent> closeRequestHandler
) {
}
