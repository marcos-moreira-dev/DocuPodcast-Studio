package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;

/** TextArea with a real multiline placeholder overlay for technical sheets. */
final class TechnicalSheetTextArea extends StackPane {
    private final TextArea textArea;

    TechnicalSheetTextArea(String initialText, String placeholder) {
        getStyleClass().add("theatre-technical-sheet-input");
        textArea = new TextArea(initialText == null ? "" : initialText);
        textArea.getStyleClass().add("theatre-character-description-area");
        textArea.setWrapText(true);
        textArea.setPromptText("");

        Label prompt = new Label(placeholder == null ? "" : placeholder);
        prompt.getStyleClass().add("theatre-technical-sheet-placeholder");
        prompt.setWrapText(true);
        prompt.setMouseTransparent(true);
        prompt.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> textArea.getText().isBlank() && !textArea.isFocused(),
                textArea.textProperty(),
                textArea.focusedProperty()));
        prompt.managedProperty().bind(prompt.visibleProperty());

        getChildren().addAll(textArea, prompt);
        StackPane.setAlignment(prompt, Pos.TOP_LEFT);
        StackPane.setMargin(prompt, new Insets(8, 10, 8, 10));
    }

    String text() {
        return textArea.getText();
    }

    TextArea textArea() {
        return textArea;
    }
}
