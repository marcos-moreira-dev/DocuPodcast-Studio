package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.AppIcon;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Permanent multimedia micro-rail that switches between image and background-audio assignment. */
public final class TheatreMultimediaLayersPane extends BorderPane {
    private final StackPane content = new StackPane();
    private final Node imagePane;
    private final Node audioPane;
    private final Runnable expandImage;
    private final Runnable expandAudio;
    private final Button imageButton;
    private final Button audioButton;
    private Mode mode = Mode.IMAGE;

    public TheatreMultimediaLayersPane(Node imagePane, Runnable expandImage, Node audioPane, Runnable expandAudio) {
        this.imagePane = imagePane;
        this.audioPane = audioPane;
        this.expandImage = expandImage == null ? () -> { } : expandImage;
        this.expandAudio = expandAudio == null ? () -> { } : expandAudio;
        getStyleClass().add("theatre-multimedia-layers");
        imageButton = railButton(AppIcon.IMAGE, "Imagen", () -> show(Mode.IMAGE));
        audioButton = railButton(AppIcon.AUDIO, "Pista de audio", () -> show(Mode.AUDIO));
        VBox rail = new VBox(10, imageButton, audioButton);
        rail.setPadding(new Insets(8, 6, 8, 6));
        rail.getStyleClass().add("theatre-multimedia-mode-rail");
        setLeft(rail);
        setCenter(content);
        show(Mode.IMAGE);
    }

    private Button railButton(AppIcon icon, String label, Runnable action) {
        Button button = ActionButtonFactory.sideDockRail(icon, label, action);
        button.setText("");
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("theatre-multimedia-mode-button");
        return button;
    }

    private void show(Mode next) {
        mode = next == null ? Mode.IMAGE : next;
        if (mode == Mode.IMAGE) expandImage.run(); else expandAudio.run();
        content.getChildren().setAll(mode == Mode.IMAGE ? imagePane : audioPane);
        imageButton.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), mode == Mode.IMAGE);
        audioButton.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), mode == Mode.AUDIO);
        if (content.getChildren().getFirst() instanceof javafx.scene.layout.Region region) {
            region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            VBox.setVgrow(region, Priority.ALWAYS);
        }
    }

    public String activeMode() {
        return mode.name();
    }

    private enum Mode { IMAGE, AUDIO }
}
