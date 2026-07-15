package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.util.Objects;
import java.util.function.BiConsumer;

/** Manual classification actions for the selected imported block. */
public final class DocumentActionsPanel extends ScrollPane {
    private final ObservableValue<ReadableDocument> documentProperty;
    private final ObservableValue<String> selectedBlockIdProperty;
    private final BiConsumer<String, DocumentBlockType> onBlockTypeChanged;
    private final VBox content = new VBox(8);

    public DocumentActionsPanel(ObservableValue<ReadableDocument> documentProperty,
                                ObservableValue<String> selectedBlockIdProperty,
                                BiConsumer<String, DocumentBlockType> onBlockTypeChanged) {
        this.documentProperty = Objects.requireNonNull(documentProperty, "documentProperty");
        this.selectedBlockIdProperty = Objects.requireNonNull(selectedBlockIdProperty, "selectedBlockIdProperty");
        this.onBlockTypeChanged = Objects.requireNonNull(onBlockTypeChanged, "onBlockTypeChanged");
        getStyleClass().add("document-side-scroll");
        content.getStyleClass().add("document-side-content");
        setFitToWidth(true);
        setContent(content);
        ChangeListener<Object> listener = (obs, oldValue, newValue) -> render();
        documentProperty.addListener(listener);
        selectedBlockIdProperty.addListener(listener);
        render();
    }

    private void render() {
        content.getChildren().clear();
        Label title = new Label("Acciones de bloque");
        title.getStyleClass().add("document-side-title");
        content.getChildren().add(title);
        ReadableDocument document = documentProperty.getValue();
        String blockId = selectedBlockIdProperty.getValue();
        if (document == null || blockId == null || blockId.isBlank()) {
            muted("Selecciona un bloque para marcarlo como título, subtítulo, párrafo o ignorado antes de preparar la lectura.");
            return;
        }
        DocumentBlock selected = document.blockById(blockId).orElse(null);
        if (selected == null) {
            muted("El bloque seleccionado ya no existe.");
            return;
        }
        muted("Bloque: " + selected.id() + " · tipo actual: " + selected.type().displayName());
        addAction("Marcar como título principal", blockId, DocumentBlockType.TITLE);
        addAction("Marcar como título", blockId, DocumentBlockType.HEADING);
        addAction("Marcar como subtítulo", blockId, DocumentBlockType.SUBHEADING);
        addAction("Marcar como párrafo", blockId, DocumentBlockType.PARAGRAPH);
        addAction("Marcar como lista", blockId, DocumentBlockType.LIST_ITEM);
        addAction("Ignorar en audio", blockId, DocumentBlockType.IGNORED);
    }

    private void addAction(String text, String blockId, DocumentBlockType type) {
        var button = ActionButtonFactory.secondary(text, () -> onBlockTypeChanged.accept(blockId, type));
        button.getStyleClass().add("document-action-button");
        content.getChildren().add(button);
    }

    private void muted(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("document-side-muted");
        content.getChildren().add(label);
    }
}
