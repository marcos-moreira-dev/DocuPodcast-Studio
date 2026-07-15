package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.AppStyles;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Reusable assistant surface for model installation/import/verification plans. */
public final class ModelInstallAssistantView extends VBox {
    public ModelInstallAssistantView() {
        super(10);
        getStyleClass().add(AppStyles.UI_MODEL_ASSISTANT);
        for (ModelInstallPlan plan : ModelInstallAssistantCatalog.recommendedPlans()) {
            getChildren().add(card(plan));
        }
    }

    private static VBox card(ModelInstallPlan plan) {
        VBox card = new VBox(8);
        card.getStyleClass().add(AppStyles.UI_MODEL_CARD);

        Label badge = new Label(plan.badge());
        badge.getStyleClass().add(AppStyles.UI_MODEL_BADGE);
        Label title = copy(plan.title(), AppStyles.UI_MODEL_TITLE);
        HBox heading = new HBox(8, badge, title);
        heading.getStyleClass().add(AppStyles.UI_MODEL_HEADING);

        VBox rows = new VBox(5,
                row("Carpeta", plan.storageFolder()),
                row("Instalación", plan.installStrategy()),
                row("Si falla descarga", plan.offlineFallback()),
                row("Verificación", plan.verification()),
                row("Flujo normal", plan.normalUserFlow()),
                row("Notas técnicas", plan.advancedNotes())
        );
        rows.getStyleClass().add(AppStyles.UI_MODEL_DETAILS);

        VBox steps = new VBox(4);
        steps.getStyleClass().add(AppStyles.UI_MODEL_STEPS);
        for (ModelInstallStep step : plan.steps()) {
            steps.getChildren().add(copy("• " + step.title() + ": " + step.detail(), AppStyles.UI_MODEL_STEP));
        }

        Label actions = copy("Acciones guiadas: " + String.join(" · ", plan.actions()), AppStyles.UI_MODEL_ACTIONS);
        Label verification = copy("Verificación local: InspectLocalModelFolderUseCase revisa carpeta, archivos mínimos y manifiesto de checksum cuando exista. Las acciones operativas reales se exponen en Configuración mediante el asistente Voz IA avanzada; esta tarjeta conserva el plan verificable y la ruta manual.", AppStyles.UI_MODEL_VALUE);
        card.getChildren().addAll(heading, rows, steps, actions, verification);
        return card;
    }

    private static HBox row(String key, String value) {
        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().add(AppStyles.UI_MODEL_KEY);
        Label valueLabel = copy(value, AppStyles.UI_MODEL_VALUE);
        HBox row = new HBox(8, keyLabel, valueLabel);
        row.getStyleClass().add(AppStyles.UI_MODEL_ROW);
        return row;
    }

    private static Label copy(String text, String styleClass) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add(styleClass);
        return label;
    }
}
