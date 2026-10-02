package com.marcosmoreiradev.docupodcaststudio.presentation.examples;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeDialogResponse;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioDialogShell;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioViewportControls;

import com.marcosmoreiradev.docupodcaststudio.application.examples.ExampleProjectDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.examples.ExampleProjectReadinessReport;
import com.marcosmoreiradev.docupodcaststudio.application.examples.InspectExampleProjectReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.InfoBadge;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.SectionHeader;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.DialogStyler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.List;
import java.util.Optional;

/** Modal chooser for bundled product demos. It selects one example and exits. */
public final class ExampleProjectDialog {
    private final List<ExampleProjectDescriptor> examples;
    private final InspectExampleProjectReadinessUseCase readiness;

    public ExampleProjectDialog(List<ExampleProjectDescriptor> examples) {
        this(examples, new InspectExampleProjectReadinessUseCase());
    }

    public ExampleProjectDialog(List<ExampleProjectDescriptor> examples, InspectExampleProjectReadinessUseCase readiness) {
        this.examples = examples == null ? List.of() : List.copyOf(examples);
        this.readiness = readiness == null ? new InspectExampleProjectReadinessUseCase() : readiness;
    }

    public Optional<ExampleProjectDescriptor> show(Window owner) {
        Dialog<ExampleProjectDescriptor> dialog = StudioDialogShell.dialog();
        dialog.setTitle("Ejemplos");
        dialog.setHeaderText("Crear proyecto demo");
        DialogStyler.apply(dialog, owner);
        ButtonType cancel = NativeDialogResponse.button("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cancel);
        dialog.getDialogPane().setContent(content(dialog));
        dialog.setResultConverter(button -> null);
        return dialog.showAndWait();
    }

    private Node content(Dialog<ExampleProjectDescriptor> dialog) {
        VBox root = new VBox(16);
        root.getStyleClass().add("examples-dialog");
        root.setPadding(new Insets(16));
        root.getChildren().add(new SectionHeader(
                "Elige un ejemplo incluido",
                "DocuPodcast copiara el Word demo y sus assets a una carpeta de proyecto que tu elijas."));

        VBox cards = new VBox(12);
        cards.getStyleClass().add("examples-card-list");
        for (ExampleProjectDescriptor example : examples) {
            cards.getChildren().add(card(dialog, example));
        }
        ScrollPane scroll = StudioViewportControls.scrollPane(cards);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(410);
        scroll.getStyleClass().add("examples-scroll");
        root.getChildren().add(scroll);
        return root;
    }

    private Node card(Dialog<ExampleProjectDescriptor> dialog, ExampleProjectDescriptor example) {
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("example-project-card");

        VBox copy = new VBox(7);
        copy.getStyleClass().add("example-project-copy");
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(example.title());
        title.getStyleClass().add("example-project-title");
        titleRow.getChildren().add(title);
        ExampleProjectReadinessReport report = readiness.inspect(example);
        titleRow.getChildren().add(new InfoBadge(report.statusLabel(), "info-badge-neutral"));
        Label subtitle = new Label(example.subtitle());
        subtitle.getStyleClass().add("example-project-subtitle");
        Label description = new Label(example.description());
        description.setWrapText(true);
        description.getStyleClass().add("example-project-description");
        Label readinessLabel = new Label(report.operationalSummary());
        readinessLabel.setWrapText(true);
        readinessLabel.getStyleClass().add("example-project-readiness");

        HBox badges = new HBox(6);
        badges.setAlignment(Pos.CENTER_LEFT);
        badges.getStyleClass().add("example-project-badges");
        for (String capability : example.capabilities()) {
            badges.getChildren().add(new InfoBadge(capability, "info-badge-neutral"));
        }
        copy.getChildren().addAll(titleRow, subtitle, description, readinessLabel, badges);
        HBox.setHgrow(copy, Priority.ALWAYS);

        Button create = ActionButtonFactory.primary("Crear proyecto demo", () -> {
            dialog.setResult(example);
            dialog.close();
        });
        create.getStyleClass().add("example-project-create");
        card.getChildren().addAll(copy, create);
        return card;
    }
}
