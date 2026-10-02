package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/** Side panel with filterable detected Word/DOCX structure. */
public final class DocumentStructurePanel extends ScrollPane {
    private final ObservableValue<ReadableDocument> documentProperty;
    private final Consumer<String> onBlockSelected;
    private final VBox content = new VBox(6);
    private final ComboBox<String> filter = StudioFormControls.comboBox();

    public DocumentStructurePanel(ObservableValue<ReadableDocument> documentProperty, Consumer<String> onBlockSelected) {
        this.documentProperty = Objects.requireNonNull(documentProperty, "documentProperty");
        this.onBlockSelected = Objects.requireNonNull(onBlockSelected, "onBlockSelected");
        getStyleClass().add("document-side-scroll");
        content.getStyleClass().add("document-side-content");
        setFitToWidth(true);
        setContent(content);
        filter.setItems(FXCollections.observableArrayList("Todos", "Estructura", "Narrables", "Imágenes", "Tablas", "Fórmulas", "Ignorados", "Advertencias"));
        filter.getSelectionModel().selectFirst();
        filter.setMaxWidth(Double.MAX_VALUE);
        filter.getStyleClass().add("document-filter-combo");
        filter.valueProperty().addListener((obs, oldValue, newValue) -> render(documentProperty.getValue()));
        ChangeListener<ReadableDocument> listener = (obs, oldValue, newValue) -> render(newValue);
        documentProperty.addListener(listener);
        render(documentProperty.getValue());
    }

    private void render(ReadableDocument document) {
        content.getChildren().clear();
        Label title = new Label("Estructura documental");
        title.getStyleClass().add("document-side-title");
        content.getChildren().add(title);
        Label filterLabel = new Label("Filtro");
        filterLabel.getStyleClass().add("document-metric-label");
        content.getChildren().addAll(filterLabel, filter);
        if (document == null) {
            Label empty = new Label("Abre un Word/DOCX para ver títulos, subtítulos, párrafos, listas, imágenes, tablas y fórmulas detectadas.");
            empty.setWrapText(true);
            empty.getStyleClass().add("document-side-muted");
            content.getChildren().add(empty);
            return;
        }
        Label summary = new Label("%d bloques · %d narrables · %d ignorados".formatted(document.blocks().size(), document.narratableBlockCount(), document.ignoredCount()));
        summary.getStyleClass().add("document-side-muted");
        content.getChildren().add(summary);
        for (DocumentBlock block : document.blocks()) {
            if (!accepts(document, block)) {
                continue;
            }
            var button = ActionButtonFactory.rail(labelFor(block), () -> onBlockSelected.accept(block.id()));
            // This label is document content with a block-type marker, not an action name.
            button.setGraphic(null);
            button.getStyleClass().addAll("document-structure-item", "document-structure-" + block.type().name().toLowerCase(Locale.ROOT));
            content.getChildren().add(button);
        }
    }

    private boolean accepts(ReadableDocument document, DocumentBlock block) {
        String value = filter.getValue();
        if (value == null || value.equals("Todos")) return true;
        return switch (value) {
            case "Estructura" -> block.structural();
            case "Narrables" -> block.narratable();
            case "Imágenes" -> block.type() == DocumentBlockType.IMAGE_NOTICE;
            case "Tablas" -> block.type() == DocumentBlockType.TABLE_NOTICE;
            case "Fórmulas" -> block.type() == DocumentBlockType.MATH_NOTICE;
            case "Ignorados" -> block.type() == DocumentBlockType.IGNORED;
            case "Advertencias" -> document.issues().stream().anyMatch(issue -> issue.blockId().equals(block.id()));
            default -> true;
        };
    }

    private static String labelFor(DocumentBlock block) {
        String prefix = switch (block.type()) {
            case TITLE -> "◉";
            case HEADING -> "▣";
            case SUBHEADING -> "▪";
            case IMAGE_NOTICE -> "▧";
            case TABLE_NOTICE -> "▦";
            case MATH_NOTICE -> "∑";
            case LIST_ITEM -> "•";
            case IGNORED -> "×";
            default -> "–";
        };
        return prefix + " " + block.id() + " · " + block.preview(78);
    }
}
