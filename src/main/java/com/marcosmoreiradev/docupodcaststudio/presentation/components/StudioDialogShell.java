package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.control.Dialog;

/** Product dialog shell; its response ButtonBar deliberately remains native JavaFX. */
public final class StudioDialogShell {
    private StudioDialogShell() { }

    public static <R> Dialog<R> dialog() {
        Dialog<R> dialog = new Dialog<>();
        dialog.getDialogPane().getStyleClass().add("product-dialog");
        return dialog;
    }
}
