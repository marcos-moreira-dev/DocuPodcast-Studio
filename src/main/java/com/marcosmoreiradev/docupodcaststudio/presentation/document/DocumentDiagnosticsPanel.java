package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentImportIssue;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.util.Locale;
import java.util.Objects;

/** Side panel with import diagnostics and Word-first warnings. */
public final class DocumentDiagnosticsPanel extends ScrollPane {
    private final ObservableValue<ReadableDocument> documentProperty;
    private final VBox content = new VBox(8);

    public DocumentDiagnosticsPanel(ObservableValue<ReadableDocument> documentProperty) {
        this.documentProperty = Objects.requireNonNull(documentProperty, "documentProperty");
        getStyleClass().add("document-side-scroll");
        content.getStyleClass().add("document-side-content");
        setFitToWidth(true);
        setContent(content);
        ChangeListener<ReadableDocument> listener = (obs, oldValue, newValue) -> render(newValue);
        documentProperty.addListener(listener);
        render(documentProperty.getValue());
    }

    private void render(ReadableDocument document) {
        content.getChildren().clear();
        Label title = new Label("Diagnóstico");
        title.getStyleClass().add("document-side-title");
        content.getChildren().add(title);
        if (document == null) {
            Label empty = new Label("Aquí aparecerán advertencias sobre títulos no detectados, imágenes sin descripción, tablas resumidas y párrafos largos.");
            empty.setWrapText(true);
            empty.getStyleClass().add("document-side-muted");
            content.getChildren().add(empty);
            return;
        }
        content.getChildren().add(metric("Formato", document.format().displayName()));
        content.getChildren().add(metric("Bloques", String.valueOf(document.blocks().size())));
        content.getChildren().add(metric("Narrables", String.valueOf(document.narratableBlockCount())));
        content.getChildren().add(metric("Estructura", String.valueOf(document.structuralBlockCount())));
        content.getChildren().add(metric("Títulos", String.valueOf(document.headingCount())));
        content.getChildren().add(metric("Subtítulos", String.valueOf(document.subheadingCount())));
        content.getChildren().add(metric("Listas", String.valueOf(document.listItemCount())));
        content.getChildren().add(metric("Imágenes", String.valueOf(document.imageNoticeCount())));
        content.getChildren().add(metric("Tablas", String.valueOf(document.tableNoticeCount())));
        content.getChildren().add(metric("Ignorados", String.valueOf(document.ignoredCount())));
        if (document.issues().isEmpty()) {
            Label ok = new Label("No se registraron advertencias de importación.");
            ok.setWrapText(true);
            ok.getStyleClass().add("document-issue-info");
            content.getChildren().add(ok);
            return;
        }
        Label issuesTitle = new Label("Advertencias y notas");
        issuesTitle.getStyleClass().add("document-side-subtitle");
        content.getChildren().add(issuesTitle);
        for (DocumentImportIssue issue : document.issues()) {
            Label row = new Label("[%s] %s%s".formatted(
                    issue.level().displayName(),
                    issue.message(),
                    issue.blockId().isBlank() ? "" : " (" + issue.blockId() + ")"
            ));
            row.setWrapText(true);
            row.getStyleClass().add("document-issue-" + issue.level().name().toLowerCase(Locale.ROOT));
            content.getChildren().add(row);
        }
    }

    private static VBox metric(String name, String value) {
        VBox box = new VBox(2);
        box.getStyleClass().add("document-metric");
        Label label = new Label(name);
        label.getStyleClass().add("document-metric-label");
        Label val = new Label(value);
        val.getStyleClass().add("document-metric-value");
        box.getChildren().addAll(label, val);
        return box;
    }
}
