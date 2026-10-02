package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;

import java.util.Objects;

/** Creates the official accessible, styled disclosure controls used by product surfaces. */
public final class StudioAccordion {
    private StudioAccordion() {
    }

    public static Accordion accordion(TitledPane... panes) {
        Accordion accordion = new Accordion(panes == null ? new TitledPane[0] : panes);
        accordion.getStyleClass().add(AppStyles.UI_ACCORDION);
        accordion.setAccessibleText("Secciones expandibles");
        return StudioControlContract.mark(accordion, StudioControlFamily.DISCLOSURE,
                StudioControlVariant.REGULAR, StudioControlDensity.REGULAR);
    }

    public static TitledPane pane(String title, Node content) {
        String accessibleTitle = title == null || title.isBlank() ? "Seccion" : title.strip();
        TitledPane pane = new TitledPane(accessibleTitle, Objects.requireNonNull(content, "content"));
        pane.getStyleClass().add(AppStyles.UI_ACCORDION_PANE);
        content.getStyleClass().add(AppStyles.UI_ACCORDION_CONTENT);
        pane.setAccessibleText(accessibleTitle + ", seccion expandible");
        pane.setAnimated(false);
        return StudioControlContract.mark(pane, StudioControlFamily.DISCLOSURE,
                StudioControlVariant.REGULAR, StudioControlDensity.REGULAR);
    }
}
