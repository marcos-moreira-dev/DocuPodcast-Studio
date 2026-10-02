package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;

/** Official scroll and split containers. */
public final class StudioViewportControls {
    private StudioViewportControls() { }

    public static ScrollPane scrollPane() { return scrollPane(null); }
    public static ScrollPane scrollPane(Node content) {
        ScrollPane control = content == null ? new ScrollPane() : new ScrollPane(content);
        control.getStyleClass().add("ui-viewport-scroll");
        return StudioControlContract.mark(control, StudioControlFamily.VIEWPORT,
                StudioControlVariant.REGULAR, StudioControlDensity.REGULAR);
    }

    public static SplitPane splitPane(Node... items) {
        SplitPane control = new SplitPane(items == null ? new Node[0] : items);
        control.getStyleClass().add("ui-viewport-split");
        return StudioControlContract.mark(control, StudioControlFamily.VIEWPORT,
                StudioControlVariant.REGULAR, StudioControlDensity.REGULAR);
    }
}
