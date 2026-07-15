package com.marcosmoreiradev.docupodcaststudio.presentation.dialogs;

import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.DialogStyler;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.util.Optional;

/** Dialog for creating a named DocuPodcast project. */
public final class ProjectNameDialog {
    private static final String DEFAULT_TITLE = "Nuevo proyecto DocuPodcast";

    public record ProjectSetup(String title, ProjectMode mode) {
        public ProjectSetup {
            title = title == null ? "" : title.trim();
            if (title.isBlank()) {
                throw new IllegalArgumentException("title is required");
            }
            mode = mode == null ? ProjectMode.defaultMode() : mode;
        }
    }

    public Optional<String> show(Window owner) {
        return showSetup(owner).map(ProjectSetup::title);
    }

    public Optional<ProjectSetup> showSetup(Window owner) {
        Dialog<ProjectSetup> dialog = new Dialog<>();
        dialog.setTitle("Nuevo proyecto");
        dialog.setHeaderText("Crear proyecto DocuPodcast");
        ButtonType createButton = new ButtonType("Crear proyecto", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(createButton, ButtonType.CANCEL);

        TextField titleField = new TextField(DEFAULT_TITLE);
        titleField.setPrefColumnCount(32);
        ComboBox<ProjectMode> modeBox = new ComboBox<>(FXCollections.observableArrayList(ProjectMode.officialModes()));
        modeBox.setMaxWidth(Double.MAX_VALUE);
        modeBox.setValue(ProjectMode.defaultMode());
        modeBox.setCellFactory(list -> modeCell());
        modeBox.setButtonCell(modeCell());

        GridPane content = new GridPane();
        content.setHgap(10);
        content.setVgap(10);
        content.add(new Label("Nombre del proyecto:"), 0, 0);
        content.add(titleField, 1, 0);
        content.add(new Label("Modo:"), 0, 1);
        content.add(modeBox, 1, 1);
        dialog.getDialogPane().setContent(content);

        Node createNode = dialog.getDialogPane().lookupButton(createButton);
        createNode.disableProperty().bind(Bindings.createBooleanBinding(
                () -> titleField.getText() == null || titleField.getText().trim().isBlank(),
                titleField.textProperty()));
        dialog.setResultConverter(button -> button == createButton
                ? new ProjectSetup(titleField.getText(), modeBox.getValue())
                : null);
        DialogStyler.apply(dialog, owner);
        return dialog.showAndWait();
    }

    private static ListCell<ProjectMode> modeCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(ProjectMode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.displayName());
            }
        };
    }
}
