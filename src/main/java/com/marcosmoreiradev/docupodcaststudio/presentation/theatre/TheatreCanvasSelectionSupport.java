package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import javafx.beans.property.StringProperty;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.function.Consumer;

/** Shared selection semantics for theatre canvases that render interventions. */
final class TheatreCanvasSelectionSupport {
    private TheatreCanvasSelectionSupport() {
    }

    static boolean clearOnEmptyPrimaryClick(
            MouseEvent event,
            StringProperty selectedIntervencion,
            Consumer<String> blockSelection) {
        if (event == null || event.getButton() != MouseButton.PRIMARY) {
            return false;
        }
        clearSelection(selectedIntervencion, blockSelection);
        event.consume();
        return true;
    }

    static void clearSelection(StringProperty selectedIntervencion, Consumer<String> blockSelection) {
        if (selectedIntervencion != null) {
            selectedIntervencion.set("");
        }
        if (blockSelection != null) {
            blockSelection.accept("");
        }
    }
}
