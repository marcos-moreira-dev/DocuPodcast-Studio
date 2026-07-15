package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import com.marcosmoreiradev.docupodcaststudio.application.services.SettingsApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.Objects;

/** Settings card for OCR/Tesseract without adding orchestration to SettingsDialog. */
final class OcrSettingsCard {
    private final OcrSettingsOperations operations;

    OcrSettingsCard(OcrSettingsOperations operations) {
        this.operations = Objects.requireNonNull(operations, "operations");
    }

    Node create(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot) {
        VBox card = new VBox(8);
        card.getStyleClass().add("settings-engine-status-card");
        Label title = new Label("OCR PDF local");
        title.getStyleClass().add("settings-engine-status-title");
        Label description = new Label("Tesseract local para convertir paginas PDF visibles en fragmentos narrables. La app usa OCR-only para seleccionar, escuchar y resaltar PDFs.");
        description.setWrapText(true);
        description.getStyleClass().add("settings-engine-status-message");
        Label status = new Label(operations.statusText(form, applicationRoot));
        status.setWrapText(true);
        status.getStyleClass().add("settings-engine-status-action");
        GridPane grid = formGrid();
        addFormRow(grid, 0, "Tesseract executable", form.ocrTesseractExecutable);
        addFormRow(grid, 1, "Idiomas OCR", form.ocrLanguages);
        addFormRow(grid, 2, "DPI OCR", form.ocrDpi);
        addFormRow(grid, 3, "Tiempo maximo OCR", form.ocrTimeoutSeconds);
        addFormRow(grid, 4, "Usar cache OCR", form.ocrCacheEnabled);
        Button verify = ActionButtonFactory.secondary("Verificar",
                () -> operations.verify(form, services, applicationRoot, status));
        Button importTesseract = ActionButtonFactory.primary("Importar carpeta",
                () -> operations.importRuntimeFolder(form, services, applicationRoot, status));
        Button downloadTesseract = ActionButtonFactory.secondary("Descargar",
                () -> operations.downloadRuntime(form, services, applicationRoot, status));
        TitledPane sources = collapsedDetails("Fuentes y rutas",
                SettingsDialog.downloadUrlControl("URL runtime OCR opcional", form.ocrTesseractRuntimeZipUrl,
                        "No se define una URL publica por defecto. Si configuras DOCUPODCAST_TESSERACT_RUNTIME_ZIP_URL o esta URL, se conserva para una descarga gestionada posterior."));
        card.getChildren().addAll(title, description, status, grid,
                actionRow(verify, importTesseract, downloadTesseract), sources);
        return card;
    }

    private static GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("settings-edit-grid");
        grid.setHgap(12);
        grid.setVgap(8);
        return grid;
    }

    private static void addFormRow(GridPane grid, int row, String label, Node field) {
        Label key = new Label(label);
        key.getStyleClass().add("settings-key");
        field.getStyleClass().add("settings-edit-control");
        grid.add(key, 0, row);
        grid.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private static HBox actionRow(Button... buttons) {
        HBox actions = new HBox(8, buttons);
        actions.getStyleClass().add("settings-engine-card-actions");
        return actions;
    }

    private static TitledPane collapsedDetails(String title, Node content) {
        TitledPane pane = new TitledPane(title, content);
        pane.setExpanded(false);
        pane.getStyleClass().add("settings-engine-details-pane");
        return pane;
    }
}
