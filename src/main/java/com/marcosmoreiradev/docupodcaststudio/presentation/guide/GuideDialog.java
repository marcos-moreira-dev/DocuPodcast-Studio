package com.marcosmoreiradev.docupodcaststudio.presentation.guide;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioDialogShell;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioNavigationControls;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioViewportControls;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioCollectionControls;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;

import com.marcosmoreiradev.docupodcaststudio.application.guide.GuideSearchResult;
import com.marcosmoreiradev.docupodcaststudio.application.guide.GuideTopic;
import com.marcosmoreiradev.docupodcaststudio.application.guide.GuideTopicId;
import com.marcosmoreiradev.docupodcaststudio.application.services.GuideApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.DialogStyler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Objects;

/** Integrated offline guide rendered as JavaFX nodes, not raw Markdown text. */
public final class GuideDialog {
    private final GuideApplicationServices services;

    public GuideDialog(GuideApplicationServices services) {
        this.services = Objects.requireNonNull(services, "services");
    }

    public void show(Window owner) {
        showTopic(owner, GuideTopicId.GETTING_STARTED);
    }

    public void showTopic(Window owner, GuideTopicId topicId) {
        Dialog<Void> dialog = StudioDialogShell.dialog();
        dialog.setTitle("Guía de DocuPodcast Studio");
        dialog.setHeaderText("Guía integrada: documento fuente, proyecto, lectura, voces, imágenes, audio y exportación");
        DialogStyler.apply(dialog, owner);
        var css = GuideDialog.class.getResource("/css/docupodcast-light.css");
        if (css != null) {
            dialog.getDialogPane().getStylesheets().add(css.toExternalForm());
        }
        dialog.getDialogPane().getStyleClass().add("guide-dialog");
        dialog.getDialogPane().setPrefSize(1060, 720);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setContent(buildContent(topicId));
        dialog.showAndWait();
    }

    private Node buildContent(GuideTopicId initialTopicId) {
        TabPane tabs = StudioNavigationControls.tabPane();
        tabs.getStyleClass().add("guide-tabs");
        tabs.getTabs().add(contentTab(initialTopicId));
        tabs.getTabs().add(searchTab());
        tabs.getTabs().forEach(tab -> tab.setClosable(false));
        return tabs;
    }

    private Tab contentTab(GuideTopicId initialTopicId) {
        ListView<GuideTopic> list = StudioCollectionControls.listView();
        list.getStyleClass().add("guide-topic-list");
        list.getItems().setAll(services.catalog().topics());
        list.setCellFactory(view -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(GuideTopic item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.category() + " · " + item.title());
            }
        });
        VBox body = guideBody();
        list.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> renderTopic(body, newValue));
        services.catalog().topic(initialTopicId).ifPresent(topic -> list.getSelectionModel().select(topic));
        if (list.getSelectionModel().getSelectedItem() == null && !list.getItems().isEmpty()) {
            list.getSelectionModel().selectFirst();
        }
        renderTopic(body, list.getSelectionModel().getSelectedItem());

        SplitPane split = StudioViewportControls.splitPane(list, wrap(body));
        split.setDividerPositions(0.28);
        Tab tab = StudioNavigationControls.tab("Contenido");
        tab.setContent(split);
        return tab;
    }

    private Tab searchTab() {
        TextField query = StudioFormControls.textField();
        query.setPromptText("Buscar: documento, proyecto, voz, imagen, audio, exportar…");
        ListView<GuideSearchResult> results = StudioCollectionControls.listView();
        results.getStyleClass().add("guide-search-results");
        results.setCellFactory(view -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(GuideSearchResult item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.topic().title() + " · puntuación " + item.score());
            }
        });
        VBox body = guideBody();
        query.textProperty().addListener((obs, oldValue, newValue) -> results.getItems().setAll(services.searchTopics().search(newValue)));
        results.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> renderTopic(body, newValue == null ? null : newValue.topic()));

        VBox left = new VBox(8, new Label("Búsqueda local"), query, results);
        VBox.setVgrow(results, Priority.ALWAYS);
        left.setPadding(new Insets(10));
        SplitPane split = StudioViewportControls.splitPane(left, wrap(body));
        split.setDividerPositions(0.34);
        Tab tab = StudioNavigationControls.tab("Buscar");
        tab.setContent(split);
        return tab;
    }

    private VBox guideBody() {
        VBox body = new VBox(8);
        body.getStyleClass().add("guide-body-rendered");
        return body;
    }

    private Node wrap(VBox body) {
        ScrollPane scroll = StudioViewportControls.scrollPane(body);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("guide-body-scroll");
        BorderPane pane = new BorderPane(scroll);
        pane.setPadding(new Insets(10));
        return pane;
    }

    private void renderTopic(VBox body, GuideTopic topic) {
        body.getChildren().clear();
        if (topic == null) {
            body.getChildren().add(paragraph("Selecciona un tema de la guía."));
            return;
        }
        body.getChildren().add(title(topic.title()));
        if (!topic.summary().isBlank()) {
            Label summary = paragraph(topic.summary());
            summary.getStyleClass().add("guide-topic-summary");
            body.getChildren().add(summary);
        }
        renderMarkup(body, topic.bodyMarkdown());
    }

    private void renderMarkup(VBox body, String source) {
        boolean inCode = false;
        StringBuilder code = new StringBuilder();
        for (String rawLine : Objects.toString(source, "").split("\\R", -1)) {
            String line = rawLine.stripTrailing();
            String trimmed = line.strip();
            if (trimmed.startsWith("```")) {
                if (inCode) {
                    body.getChildren().add(codeBlock(code.toString().stripTrailing()));
                    code.setLength(0);
                    inCode = false;
                } else {
                    inCode = true;
                }
                continue;
            }
            if (inCode) {
                code.append(line).append('\n');
                continue;
            }
            if (trimmed.isBlank()) {
                continue;
            }
            if (trimmed.startsWith("# ")) {
                continue; // The dialog already renders the topic title.
            }
            if (trimmed.startsWith("## ")) {
                body.getChildren().add(heading(trimmed.substring(3).strip()));
            } else if (trimmed.startsWith("### ")) {
                body.getChildren().add(subheading(trimmed.substring(4).strip()));
            } else if (trimmed.startsWith("- ")) {
                body.getChildren().add(bullet(trimmed.substring(2).strip()));
            } else if (trimmed.matches("^\\d+\\.\\s+.+")) {
                body.getChildren().add(bullet(trimmed));
            } else {
                body.getChildren().add(paragraph(trimmed));
            }
        }
        if (inCode && !code.isEmpty()) {
            body.getChildren().add(codeBlock(code.toString().stripTrailing()));
        }
    }

    private Label title(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("guide-topic-title-rendered");
        return label;
    }

    private Label heading(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("guide-topic-heading");
        return label;
    }

    private Label subheading(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("guide-topic-subheading");
        return label;
    }

    private Label paragraph(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("guide-topic-paragraph");
        return label;
    }

    private Label bullet(String text) {
        Label label = new Label("• " + text);
        label.setWrapText(true);
        label.getStyleClass().add("guide-topic-bullet");
        return label;
    }

    private Label codeBlock(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("guide-topic-code");
        return label;
    }
}
