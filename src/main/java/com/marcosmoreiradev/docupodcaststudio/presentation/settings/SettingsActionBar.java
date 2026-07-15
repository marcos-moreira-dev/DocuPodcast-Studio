package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.OperationalStatusStrip;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.Objects;
import java.util.function.Consumer;

/** Operational bottom bar for SettingsDialog: state + restore + save, no decorative filler. */
public final class SettingsActionBar {
    private SettingsActionBar() {
    }

    public static Node create(Runnable restoreDefaultsAction,
                              String initialStatus,
                              boolean saveAvailable,
                              Consumer<Label> saveHandler) {
        Objects.requireNonNull(restoreDefaultsAction, "restoreDefaultsAction");
        Objects.requireNonNull(saveHandler, "saveHandler");
        HBox bar = new HBox(10);
        bar.getStyleClass().add("settings-action-bar");
        bar.setPadding(new Insets(10, 16, 10, 16));

        OperationalStatusStrip status = new OperationalStatusStrip(initialStatus);
        status.getStyleClass().add("settings-save-status-strip");
        status.messageLabel().getStyleClass().add("settings-save-status");
        HBox.setHgrow(status, Priority.ALWAYS);

        Button restore = ActionButtonFactory.secondary("Restaurar predeterminados", () -> {
            restoreDefaultsAction.run();
            status.setMessage("Valores predeterminados cargados en pantalla. Pulsa Guardar cambios para persistirlos.");
        });
        restore.setMinWidth(190);

        Button save = ActionButtonFactory.primary("Guardar cambios", () -> saveHandler.accept(status.messageLabel()));
        save.setMinWidth(150);
        save.setDisable(!saveAvailable);
        bar.getChildren().addAll(status, restore, save);
        return bar;
    }
}
