package com.marcosmoreiradev.docupodcaststudio.presentation.notification;

import javafx.scene.control.Dialog;
import javafx.stage.Window;

/** Applies owner and common styling hooks to product dialogs. */
public final class DialogStyler {
    private DialogStyler() {
    }

    public static void apply(Dialog<?> dialog, Window owner) {
        if (dialog == null) {
            return;
        }
        if (owner != null) {
            dialog.initOwner(owner);
            if (owner.getScene() != null) {
                owner.getScene().getStylesheets().stream()
                        .filter(style -> !dialog.getDialogPane()
                                .getStylesheets().contains(style))
                        .forEach(dialog.getDialogPane().getStylesheets()::add);
            }
        }
        dialog.getDialogPane().getStyleClass().add("product-dialog");
        dialog.setResizable(true);
    }
}
