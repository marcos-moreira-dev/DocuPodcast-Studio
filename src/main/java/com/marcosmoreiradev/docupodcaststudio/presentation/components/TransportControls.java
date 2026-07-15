package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.layout.HBox;

/** Shared pause/resume/stop controls for playback-like surfaces. */
public final class TransportControls extends HBox {
    public TransportControls(Runnable onPause, Runnable onResume, Runnable onStop) {
        super(6);
        getStyleClass().add(AppStyles.UI_TRANSPORT_CONTROLS);
        getChildren().addAll(
                ActionButtonFactory.transportIcon(AppIcon.PAUSE, "Pausar: detener temporalmente la lectura actual.", onPause),
                ActionButtonFactory.transportIcon(AppIcon.RESUME, "Reanudar: continuar la lectura desde el punto actual.", onResume),
                ActionButtonFactory.transportIcon(AppIcon.STOP, "Detener: cancelar la reproducción actual.", onStop)
        );
    }
}
