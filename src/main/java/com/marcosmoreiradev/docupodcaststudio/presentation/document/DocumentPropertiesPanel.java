package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.util.Objects;

/** Side panel with properties of the imported document and selected block. */
public final class DocumentPropertiesPanel extends ScrollPane {
    private final ObservableValue<ReadableDocument> documentProperty;
    private final ObservableValue<String> selectedBlockIdProperty;
    private final VBox content = new VBox(8);

    public DocumentPropertiesPanel(ObservableValue<ReadableDocument> documentProperty, ObservableValue<String> selectedBlockIdProperty) {
        this.documentProperty = Objects.requireNonNull(documentProperty, "documentProperty");
        this.selectedBlockIdProperty = Objects.requireNonNull(selectedBlockIdProperty, "selectedBlockIdProperty");
        getStyleClass().add("document-side-scroll");
        content.getStyleClass().add("document-side-content");
        setFitToWidth(true);
        setContent(content);
        ChangeListener<Object> listener = (obs, oldValue, newValue) -> render(documentProperty.getValue(), selectedBlockIdProperty.getValue());
        documentProperty.addListener(listener);
        selectedBlockIdProperty.addListener(listener);
        render(documentProperty.getValue(), selectedBlockIdProperty.getValue());
    }

    private void render(ReadableDocument document, String selectedBlockId) {
        content.getChildren().clear();
        Label title = new Label("Propiedades");
        title.getStyleClass().add("document-side-title");
        content.getChildren().add(title);
        if (document == null) {
            addMuted("No hay documento importado.");
            return;
        }
        content.getChildren().add(metric("Título", document.title()));
        content.getChildren().add(metric("Formato", document.format().displayName()));
        content.getChildren().add(metric("Archivo fuente", document.sourcePath().getFileName().toString()));
        DocumentBlock selected = document.blocks().stream()
                .filter(block -> block.id().equals(selectedBlockId))
                .findFirst()
                .orElse(null);
        if (selected == null) {
            addMuted("Selecciona un bloque en la estructura o en el documento para revisar sus propiedades.");
            return;
        }
        Label blockTitle = new Label("Bloque seleccionado");
        blockTitle.getStyleClass().add("document-side-subtitle");
        content.getChildren().add(blockTitle);
        content.getChildren().add(metric("ID", selected.id()));
        content.getChildren().add(metric("Tipo", selected.type().displayName()));
        content.getChildren().add(metric("Estilo Word", selected.originalStyle().isBlank() ? "—" : selected.originalStyle()));
        content.getChildren().add(metric("Caracteres", String.valueOf(selected.text().length())));
        content.getChildren().add(metric("Palabras", String.valueOf(selected.wordCount())));
        content.getChildren().add(metric("Narrable", selected.narratable() ? "sí" : "no"));
        if (selected.metadata().isEmpty()) {
            addMuted("Sin metadatos adicionales.");
        } else {
            Label meta = new Label("Metadatos");
            meta.getStyleClass().add("document-side-subtitle");
            content.getChildren().add(meta);
            selected.metadata().forEach((key, value) -> content.getChildren().add(metric(key, value)));
        }
    }

    private void addMuted(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("document-side-muted");
        content.getChildren().add(label);
    }

    private static VBox metric(String name, String value) {
        VBox box = new VBox(2);
        box.getStyleClass().add("document-metric");
        Label label = new Label(name);
        label.getStyleClass().add("document-metric-label");
        Label val = new Label(value == null || value.isBlank() ? "—" : value);
        val.setWrapText(true);
        val.getStyleClass().add("document-metric-value");
        box.getChildren().addAll(label, val);
        return box;
    }
}
