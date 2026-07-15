package com.marcosmoreiradev.docupodcaststudio.presentation.toolbar;

import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ToolbarActionButton;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellView;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceCapability;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceCapabilityPolicy;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/** Global and contextual toolbar for DocuPodcast Studio. */
public final class MainToolbarView extends VBox {
    private final ScrollPane contextualScroll;
    private final WorkspaceToolbarActionProvider actionProvider = WorkspaceToolbarActionProvider.official();
    private final WorkspaceCapabilityPolicy capabilityPolicy = new WorkspaceCapabilityPolicy();

    public MainToolbarView(DocuPodcastShellViewModel viewModel, DocuPodcastShellView shellView) {
        getStyleClass().add("main-toolbar-stack");
        contextualScroll = scrollableToolbarRow(new HBox(), "workspace-toolbar-scroll");
        getChildren().addAll(
                scrollableToolbarRow(globalRow(viewModel, shellView), "main-toolbar-scroll"),
                contextualScroll
        );
        refreshContextualRow(viewModel, shellView, viewModel.activeWorkspaceProperty().get());
        viewModel.activeWorkspaceProperty().addListener((obs, oldValue, newValue) -> refreshContextualRow(viewModel, shellView, newValue));
    }

    private void refreshContextualRow(DocuPodcastShellViewModel viewModel, DocuPodcastShellView shellView, WorkspaceKind workspaceKind) {
        contextualScroll.setContent(contextualRow(viewModel, shellView, workspaceKind));
        contextualScroll.setHvalue(0.0);
    }

    private HBox globalRow(DocuPodcastShellViewModel viewModel, DocuPodcastShellView shellView) {
        HBox row = toolbarRow("main-toolbar");

        Button inicio = toolbarButton("IN", "Inicio", "Volver a la pantalla de inicio", () -> shellView.dispatchCommand(AppCommandId.SHOW_WELCOME), false);
        Button abrirDocumento = toolbarButton("DOC", "Abrir fuente", "Abrir fuente documental compatible", () -> shellView.dispatchCommand(AppCommandId.OPEN_SOURCE_DOCUMENT), true);
        Button escuchar = toolbarButton("PLAY", viewModel.documentPrimaryActionLabelProperty(),
                "Iniciar, preparar o continuar la lectura en voz alta del documento", () -> shellView.dispatchCommand(AppCommandId.LISTEN_DOCUMENT), true);
        escuchar.disableProperty().bind(capabilityPolicy.disabledBinding(WorkspaceCapability.LISTEN_DOCUMENT, viewModel));
        Button exportar = toolbarButton("OUT", "Exportar audio", "Exportar audio final en WAV, MP3 o AAC", () -> shellView.dispatchCommand(AppCommandId.EXPORT_PODCAST_WAV), false);
        exportar.disableProperty().bind(capabilityPolicy.disabledBinding(WorkspaceCapability.EXPORT_PODCAST_WAV, viewModel));
        Button pantallaCompleta = toolbarButton("VIEW", "Pantalla completa", "Alternar pantalla completa", () -> shellView.dispatchCommand(AppCommandId.TOGGLE_FULLSCREEN), false);

        row.getChildren().addAll(
                groupLabel("Documento"), inicio, abrirDocumento,
                separator(), groupLabel("Lectura"), escuchar,
                separator(), groupLabel("Salida"), exportar,
                separator(), groupLabel("Vista"), pantallaCompleta
        );
        return row;
    }

    private HBox contextualRow(DocuPodcastShellViewModel viewModel, DocuPodcastShellView shellView, WorkspaceKind workspaceKind) {
        HBox row = toolbarRow("workspace-toolbar-area");
        List<WorkspaceToolbarAction> actions = actionProvider.actionsFor(workspaceKind);
        String lastGroup = "";
        boolean firstGroup = true;
        for (WorkspaceToolbarAction action : actions) {
            if (!action.group().equals(lastGroup)) {
                if (!firstGroup) {
                    row.getChildren().add(separator());
                }
                row.getChildren().add(groupLabel(action.group()));
                lastGroup = action.group();
                firstGroup = false;
            }
            Button button = action.capability() == WorkspaceCapability.LISTEN_DOCUMENT
                    ? toolbarButton(iconFor(action.capability()), viewModel.documentPrimaryActionLabelProperty(), tooltipFor(action.capability()), null, action.primary())
                    : toolbarButton(iconFor(action.capability()), action.label(), tooltipFor(action.capability()), null, action.primary());
            button.disableProperty().bind(capabilityPolicy.disabledBinding(action.capability(), viewModel));
            button.setOnAction(event -> shellView.dispatchCommand(action.commandId()));
            row.getChildren().add(button);
        }
        return row;
    }



    private HBox toolbarRow(String styleClass) {
        HBox row = new HBox(9);
        row.setPadding(new Insets(6, 10, 6, 10));
        row.getStyleClass().add(styleClass);
        return row;
    }

    private ScrollPane scrollableToolbarRow(HBox row, String styleClass) {
        ScrollPane scrollPane = new ScrollPane(row);
        scrollPane.getStyleClass().add(styleClass);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPannable(true);
        scrollPane.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, event -> {
            double delta = event.getDeltaY() == 0 ? event.getDeltaX() : event.getDeltaY();
            if (delta != 0 && scrollPane.getHmax() > 0) {
                double next = scrollPane.getHvalue() - delta / 900.0;
                scrollPane.setHvalue(Math.max(0.0, Math.min(scrollPane.getHmax(), next)));
                event.consume();
            }
        });
        VBox.setVgrow(scrollPane, Priority.NEVER);
        return scrollPane;
    }

    private Button toolbarButton(String icon, String text, String tooltip, Runnable action, boolean primary) {
        return toolbarButton(icon, new javafx.beans.property.SimpleStringProperty(text), tooltip, action, primary);
    }

    private Button toolbarButton(String icon, ObservableValue<String> text, String tooltip, Runnable action, boolean primary) {
        Button button = new ToolbarActionButton(icon, text, tooltip, action, primary);
        button.setMinWidth(Region.USE_PREF_SIZE);
        String currentText = text.getValue() == null ? "" : text.getValue();
        button.setPrefWidth(Math.max(108, 9.5 * currentText.length() + 56));
        button.setMaxWidth(Region.USE_PREF_SIZE);
        return button;
    }

    private Label groupLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("toolbar-group-label");
        label.setMinWidth(Region.USE_PREF_SIZE);
        return label;
    }

    private Region separator() {
        Region region = new Region();
        region.getStyleClass().add("toolbar-separator");
        region.setMinWidth(1);
        region.setPrefWidth(1);
        region.setMaxWidth(1);
        return region;
    }

    private String iconFor(WorkspaceCapability capability) {
        return switch (capability) {
            case IMPORT_WORD -> "DOC";
            case LISTEN_DOCUMENT, PLAY_SELECTION -> "PLAY";
            case CREATE_SCRIPT, CONFIGURE_READING_PROFILE -> "READ";
            case GENERATE_AUDIO, EXPORT_PODCAST_WAV -> "AUDIO";
            case CANCEL_AUDIO_JOB -> "STOP";
            case EXPORT_PROJECT_BUNDLE -> "OUT";
            case EXPORT_DIAGNOSTIC_REPORT -> "✓";
            case OPEN_GUIDE, OPEN_WORD_GUIDE -> "HELP";
            case OPEN_VOICE_LIBRARY, PREPARE_AI_VOICE, PREPARE_HUMAN_VOICE, IMPORT_VOICE_SAMPLE -> "VOICE";
            case CREATE_STORYBOARD, IMPORT_STORYBOARD_IMAGE, ASSOCIATE_STORYBOARD_IMAGE -> "IMG";
            default -> "";
        };
    }

    private String tooltipFor(WorkspaceCapability capability) {
        return switch (capability) {
            case IMPORT_WORD -> "Abrir fuente documental compatible";
            case LISTEN_DOCUMENT -> "Escuchar el documento desde el flujo principal";
            case CREATE_SCRIPT -> "Preparar la lectura interna del documento";
            case CONFIGURE_READING_PROFILE -> "Ajustar cómo se interpreta el documento fuente";
            case GENERATE_AUDIO -> "Generar audio por segmentos";
            case EXPORT_PROJECT_BUNDLE -> "Exportar paquete de soporte";
            case OPEN_GUIDE, OPEN_WORD_GUIDE -> "Abrir guía de uso";
            case OPEN_VOICE_LIBRARY -> "Abrir biblioteca de voces";
            case PREPARE_AI_VOICE -> "Preparar Voz IA para el fragmento";
            case PREPARE_HUMAN_VOICE -> "Preparar grabación o voz humana";
            case PLAY_SELECTION -> "Reproducir desde la selección";
            case IMPORT_VOICE_SAMPLE -> "Importar muestra de voz";
            case CREATE_STORYBOARD -> "Crear secuencia visual desde imágenes";
            case IMPORT_STORYBOARD_IMAGE -> "Importar imagen para el panel visual";
            case ASSOCIATE_STORYBOARD_IMAGE -> "Asociar imagen al fragmento";
            case CANCEL_AUDIO_JOB -> "Cancelar trabajo de audio activo";
            case EXPORT_PODCAST_WAV -> "Exportar audio final";
            case EXPORT_DIAGNOSTIC_REPORT -> "Exportar reporte de soporte";
            default -> "Acción contextual";
        };
    }
}
