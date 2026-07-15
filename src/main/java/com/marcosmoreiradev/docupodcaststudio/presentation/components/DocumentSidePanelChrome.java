package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Shared header chrome for the document side panels. */
public final class DocumentSidePanelChrome {
    private DocumentSidePanelChrome() {
    }

    public record Header(HBox node, VBox copy) {
    }

    public static Header header(String title, String summary, Button toggle) {
        Label titleLabel = new Label(title == null || title.isBlank() ? "Panel" : title);
        titleLabel.getStyleClass().add(AppStyles.UI_DOCUMENT_SIDE_PANEL_TITLE);

        Label summaryLabel = new Label(summary == null ? "" : summary);
        summaryLabel.setWrapText(true);
        summaryLabel.getStyleClass().add(AppStyles.UI_DOCUMENT_SIDE_PANEL_SUMMARY);
        boolean hasSummary = summary != null && !summary.isBlank();
        summaryLabel.setVisible(hasSummary);
        summaryLabel.setManaged(hasSummary);

        VBox copy = new VBox(2, titleLabel, summaryLabel);
        copy.getStyleClass().add(AppStyles.UI_DOCUMENT_SIDE_PANEL_COPY);
        HBox.setHgrow(copy, Priority.ALWAYS);

        HBox header = new HBox(8, copy, toggle);
        header.getStyleClass().add(AppStyles.UI_DOCUMENT_SIDE_PANEL_HEADER);
        header.setAlignment(Pos.CENTER_LEFT);
        return new Header(header, copy);
    }
}
