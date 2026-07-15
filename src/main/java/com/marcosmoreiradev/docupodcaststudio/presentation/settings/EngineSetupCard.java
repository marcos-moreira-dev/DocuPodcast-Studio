package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.AppStyles;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Reusable card for guided voice engine setup inside Settings. */
public final class EngineSetupCard extends VBox {
    public EngineSetupCard(EngineSetupOption option) {
        super(7);
        getStyleClass().add(AppStyles.UI_ENGINE_CARD);

        Label badge = new Label(option.badge());
        badge.getStyleClass().add(AppStyles.UI_ENGINE_BADGE);
        Label title = new Label(option.title());
        title.getStyleClass().add(AppStyles.UI_ENGINE_TITLE);
        HBox heading = new HBox(8, badge, title);
        heading.getStyleClass().add(AppStyles.UI_ENGINE_HEADING);

        Label summary = copy(option.summary(), AppStyles.UI_ENGINE_SUMMARY);
        VBox details = new VBox(5,
                row("Uso recomendado", option.recommendedFor()),
                row("Modelos", option.modelStorage()),
                row("Configuración guiada", option.guidedSetup()),
                row("Controles", option.supportedControls()),
                row("Disponibilidad", option.availabilityPolicy())
        );
        details.getStyleClass().add(AppStyles.UI_ENGINE_DETAILS);

        Label guide = copy("Guía informativa, no botón: estas tarjetas explican opciones, pero la preparación real se hace con los asistentes superiores.", AppStyles.UI_ENGINE_ACTIONS);
        Label actions = copy("Siguiente acción sugerida: " + String.join(" · ", option.nextActions()), AppStyles.UI_ENGINE_ACTIONS);
        getChildren().addAll(heading, summary, details, guide, actions);
    }

    private static HBox row(String key, String value) {
        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().add(AppStyles.UI_ENGINE_KEY);
        Label valueLabel = copy(value, AppStyles.UI_ENGINE_VALUE);
        HBox row = new HBox(8, keyLabel, valueLabel);
        row.getStyleClass().add(AppStyles.UI_ENGINE_ROW);
        return row;
    }

    private static Label copy(String text, String styleClass) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add(styleClass);
        return label;
    }
}
