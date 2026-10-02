package com.marcosmoreiradev.docupodcaststudio.presentation.command;

import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectModeCapabilities;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;

import java.util.Objects;

/**
 * Single runtime availability policy for commands exposed by menu bar, ribbon,
 * legacy toolbar adapters and product surfaces.
 *
 * <p>TC1 keeps command enablement in one place so two surfaces cannot disagree
 * about the same user-facing action. The policy returns JavaFX bindings for UI
 * controls and also exposes a human-readable reason for future status/tooltips.</p>
 */
public final class CommandAvailabilityPolicy {
    public BooleanBinding disabledBinding(AppCommandId commandId, DocuPodcastShellViewModel viewModel) {
        Objects.requireNonNull(commandId, "commandId");
        Objects.requireNonNull(viewModel, "viewModel");
        BooleanBinding stateDisabled = stateDisabledBinding(commandId, viewModel);
        return Bindings.createBooleanBinding(
                () -> !isVisible(commandId, viewModel) || stateDisabled.get(),
                stateDisabled,
                viewModel.currentProjectModeProperty(),
                viewModel.projectOpenProperty());
    }

    public BooleanBinding visibleBinding(AppCommandId commandId, DocuPodcastShellViewModel viewModel) {
        Objects.requireNonNull(commandId, "commandId");
        Objects.requireNonNull(viewModel, "viewModel");
        return Bindings.createBooleanBinding(
                () -> isVisible(commandId, viewModel),
                viewModel.currentProjectModeProperty(),
                viewModel.projectOpenProperty());
    }

    public StringBinding unavailableReasonBinding(AppCommandId commandId, DocuPodcastShellViewModel viewModel) {
        Objects.requireNonNull(commandId, "commandId");
        Objects.requireNonNull(viewModel, "viewModel");
        return Bindings.createStringBinding(
                () -> unavailableReason(commandId, viewModel),
                viewModel.currentProjectModeProperty(),
                viewModel.projectOpenProperty(),
                viewModel.saveableProjectOpenProperty(),
                viewModel.dirtyProperty(),
                viewModel.theatreRefreshRunningProperty(),
                viewModel.currentDocumentProperty(),
                viewModel.currentScriptProperty(),
                viewModel.selectedScriptSegmentIdProperty(),
                viewModel.selectedDocumentBlockIdProperty(),
                viewModel.selectedDocumentTextRangeProperty(),
                viewModel.audioJobRunningProperty());
    }

    private BooleanBinding stateDisabledBinding(AppCommandId commandId, DocuPodcastShellViewModel viewModel) {
        BooleanBinding commandSpecific = switch (commandId) {
            case OPEN_SOURCE_DOCUMENT, SAVE_PROJECT, SAVE_PROJECT_AS, CLOSE_PROJECT, EXPORT_DIAGNOSTIC_REPORT,
                    INSPECT_EXPORT_READINESS, IMPORT_VOICE_SAMPLE -> viewModel.projectOpenProperty().not();
            case OPEN_NARRATIVE_VISUAL_PRODUCTION -> viewModel.projectOpenProperty().not();
            case OPEN_EXPORT_CENTER, EXPORT_PODCAST_WAV, EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO, EXPORT_SIMPLE_VIDEO_PACKAGE, EXPORT_THEATRE_WORK,
                    EXPORT_THEATRE_SPATIAL_VIEW, EXPORT_THEATRE_PORTION -> viewModel.projectOpenProperty().not();
            case EXPORT_PROJECT_BUNDLE, OPEN_THEATRE_IMAGE_GENERATION ->
                    viewModel.saveableProjectOpenProperty().not();
            case REFRESH_THEATRE_PACKAGE -> viewModel.saveableProjectOpenProperty().not()
                    .or(viewModel.dirtyProperty())
                    .or(viewModel.theatreRefreshRunningProperty());
            case OPEN_PROJECT_FOLDER, OPEN_EXPORTS_FOLDER, INSPECT_PROJECT_INTEGRITY ->
                    viewModel.saveableProjectOpenProperty().not();
            case LISTEN_DOCUMENT -> viewModel.currentDocumentProperty().isNull();
            case OPEN_SOURCE_DOCUMENT_LOCATION, REFRESH_SOURCE_DOCUMENT, PREPARE_DOCUMENT_READING,
                    PREPARE_TECHNICAL_PROBLEM ->
                    viewModel.currentDocumentProperty().isNull();
            case PLAY_SELECTION, ASSIGN_AI_VOICE_TO_SELECTION, ASSIGN_HUMAN_RECORDING_TO_SELECTION,
                    IMPORT_AUDIO_FOR_SELECTION, EXTRACT_VIDEO_AUDIO_FOR_SELECTION,
                    IMPORT_IMAGE_FOR_SELECTION, IMPORT_BRIDGE_IMAGE_FOR_SELECTION,
                    ASSOCIATE_IMAGE_TO_SELECTION -> selectionUnavailableBinding(viewModel);
            case CLEAR_SELECTION -> noSelectionBinding(viewModel);
            case GENERATE_AUDIO -> viewModel.currentScriptProperty().isNull()
                    .or(viewModel.audioJobRunningProperty());
            case CREATE_STORYBOARD -> viewModel.currentScriptProperty().isNull();
            case CANCEL_AUDIO_JOB -> viewModel.audioJobRunningProperty().not();
            default -> Bindings.createBooleanBinding(() -> false);
        };
        return commandSpecific.or(viewModel.theatreRefreshRunningProperty());
    }

    public boolean isDisabled(AppCommandId commandId, DocuPodcastShellViewModel viewModel) {
        Objects.requireNonNull(commandId, "commandId");
        Objects.requireNonNull(viewModel, "viewModel");
        return !isVisible(commandId, viewModel) || viewModel.theatreRefreshRunningProperty().get() || switch (commandId) {
            case OPEN_SOURCE_DOCUMENT, SAVE_PROJECT, SAVE_PROJECT_AS, CLOSE_PROJECT, EXPORT_DIAGNOSTIC_REPORT,
                    INSPECT_EXPORT_READINESS, IMPORT_VOICE_SAMPLE -> !viewModel.projectOpenProperty().get();
            case OPEN_NARRATIVE_VISUAL_PRODUCTION -> !viewModel.projectOpenProperty().get();
            case OPEN_EXPORT_CENTER, EXPORT_PODCAST_WAV, EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO, EXPORT_SIMPLE_VIDEO_PACKAGE, EXPORT_THEATRE_WORK,
                    EXPORT_THEATRE_SPATIAL_VIEW, EXPORT_THEATRE_PORTION -> !viewModel.projectOpenProperty().get();
            case EXPORT_PROJECT_BUNDLE, OPEN_THEATRE_IMAGE_GENERATION ->
                    !viewModel.saveableProjectOpenProperty().get();
            case REFRESH_THEATRE_PACKAGE -> !viewModel.saveableProjectOpenProperty().get()
                    || viewModel.dirtyProperty().get()
                    || viewModel.theatreRefreshRunningProperty().get();
            case OPEN_PROJECT_FOLDER, OPEN_EXPORTS_FOLDER, INSPECT_PROJECT_INTEGRITY ->
                    !viewModel.saveableProjectOpenProperty().get();
            case LISTEN_DOCUMENT -> viewModel.currentDocumentProperty().get() == null;
            case OPEN_SOURCE_DOCUMENT_LOCATION, REFRESH_SOURCE_DOCUMENT, PREPARE_DOCUMENT_READING,
                    PREPARE_TECHNICAL_PROBLEM ->
                    viewModel.currentDocumentProperty().get() == null;
            case PLAY_SELECTION, ASSIGN_AI_VOICE_TO_SELECTION, ASSIGN_HUMAN_RECORDING_TO_SELECTION,
                    IMPORT_AUDIO_FOR_SELECTION, EXTRACT_VIDEO_AUDIO_FOR_SELECTION,
                    IMPORT_IMAGE_FOR_SELECTION, IMPORT_BRIDGE_IMAGE_FOR_SELECTION,
                    ASSOCIATE_IMAGE_TO_SELECTION -> selectionUnavailable(viewModel);
            case CLEAR_SELECTION -> noSelection(viewModel);
            case GENERATE_AUDIO -> viewModel.currentScriptProperty().get() == null
                    || viewModel.audioJobRunningProperty().get();
            case CREATE_STORYBOARD -> viewModel.currentScriptProperty().get() == null;
            case CANCEL_AUDIO_JOB -> !viewModel.audioJobRunningProperty().get();
            default -> false;
        };
    }

    public boolean isVisible(AppCommandId commandId, DocuPodcastShellViewModel viewModel) {
        Objects.requireNonNull(commandId, "commandId");
        Objects.requireNonNull(viewModel, "viewModel");
        ProjectModeCapabilities capabilities = ProjectModeCapabilities.forMode(viewModel.currentProjectModeProperty().get());
        return switch (commandId) {
            case OPEN_THEATRE_SCRIPT, OPEN_THEATRE_IMAGE_GENERATION, IMPORT_THEATRE_GRAMMAR,
                    REFRESH_THEATRE_PACKAGE, EXPORT_THEATRE_GRAMMAR_TEMPLATE, EXPORT_THEATRE_WORK, EXPORT_THEATRE_SPATIAL_VIEW,
                    EXPORT_THEATRE_PORTION -> capabilities.theatreProduction();
            case OPEN_NARRATIVE_VISUAL_PRODUCTION -> viewModel.currentProjectModeProperty().get() == ProjectMode.NARRATIVE_VIDEO;
            case IMPORT_NARRATIVE_VIDEO_GRAMMAR, EXPORT_NARRATIVE_VIDEO_GRAMMAR_TEMPLATE ->
                    viewModel.currentProjectModeProperty().get() == ProjectMode.NARRATIVE_VIDEO;
            case IMPORT_IMAGE_FOR_SELECTION, ASSOCIATE_IMAGE_TO_SELECTION, CREATE_STORYBOARD,
                    IMPORT_BRIDGE_IMAGE_FOR_SELECTION -> capabilities.narrativeVisuals();
            case EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO -> viewModel.currentProjectModeProperty().get() == ProjectMode.DOCUMENTARY_STUDIO;
            case PREPARE_TECHNICAL_PROBLEM -> viewModel.currentProjectModeProperty().get() == ProjectMode.DOCUMENTARY_STUDIO;
            case EXPORT_SIMPLE_VIDEO_PACKAGE -> viewModel.currentProjectModeProperty().get() == ProjectMode.NARRATIVE_VIDEO;
            default -> true;
        };
    }

    public String unavailableReason(AppCommandId commandId, DocuPodcastShellViewModel viewModel) {
        Objects.requireNonNull(commandId, "commandId");
        Objects.requireNonNull(viewModel, "viewModel");
        if (!isDisabled(commandId, viewModel)) {
            return "";
        }
        if (!isVisible(commandId, viewModel)) {
            return "El modo " + viewModel.currentProjectModeProperty().get().displayName() + " no incluye esa herramienta.";
        }
        if (viewModel.theatreRefreshRunningProperty().get()) {
            return "Espera a que termine la actualización segura de la obra.";
        }
        return switch (commandId) {
            case OPEN_SOURCE_DOCUMENT, SAVE_PROJECT, SAVE_PROJECT_AS, CLOSE_PROJECT, EXPORT_DIAGNOSTIC_REPORT,
                    INSPECT_EXPORT_READINESS, IMPORT_VOICE_SAMPLE -> "Abre o crea un proyecto primero.";
            case OPEN_NARRATIVE_VISUAL_PRODUCTION -> "Abre o crea un proyecto de Video narrativo primero.";
            case OPEN_EXPORT_CENTER, EXPORT_PODCAST_WAV, EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO, EXPORT_SIMPLE_VIDEO_PACKAGE, EXPORT_THEATRE_WORK,
                    EXPORT_THEATRE_SPATIAL_VIEW, EXPORT_THEATRE_PORTION -> "Abre o crea un proyecto primero.";
            case EXPORT_PROJECT_BUNDLE, OPEN_THEATRE_IMAGE_GENERATION ->
                    "Guarda el proyecto en una carpeta contenedora antes de exportar.";
            case REFRESH_THEATRE_PACKAGE -> viewModel.theatreRefreshRunningProperty().get()
                    ? "La obra ya se está actualizando."
                    : viewModel.dirtyProperty().get()
                    ? "Guarda los cambios pendientes antes de refrescar la obra."
                    : "Guarda el proyecto en una carpeta contenedora antes de vincular la obra.";
            case OPEN_PROJECT_FOLDER, OPEN_EXPORTS_FOLDER, INSPECT_PROJECT_INTEGRITY ->
                    "Guarda el proyecto en una carpeta contenedora primero.";
            case LISTEN_DOCUMENT -> "Abre una fuente documental primero.";
            case OPEN_SOURCE_DOCUMENT_LOCATION, REFRESH_SOURCE_DOCUMENT, PREPARE_DOCUMENT_READING,
                    PREPARE_TECHNICAL_PROBLEM ->
                    "Abre una fuente documental primero.";
            case PLAY_SELECTION, ASSIGN_AI_VOICE_TO_SELECTION, ASSIGN_HUMAN_RECORDING_TO_SELECTION,
                    IMPORT_AUDIO_FOR_SELECTION, EXTRACT_VIDEO_AUDIO_FOR_SELECTION,
                    IMPORT_IMAGE_FOR_SELECTION, IMPORT_BRIDGE_IMAGE_FOR_SELECTION,
                    ASSOCIATE_IMAGE_TO_SELECTION,
                    CLEAR_SELECTION -> "Selecciona una oración o fragmento del documento.";
            case GENERATE_AUDIO -> viewModel.audioJobRunningProperty().get()
                    ? "La generación de audio actual ya está en curso."
                    : "Prepara la lectura del documento primero.";
            case CREATE_STORYBOARD -> "Prepara la lectura del documento primero.";
            case CANCEL_AUDIO_JOB -> "No hay una generación de audio activa.";
            default -> "";
        };
    }

    private static BooleanBinding selectionUnavailableBinding(DocuPodcastShellViewModel viewModel) {
        return Bindings.createBooleanBinding(
                () -> selectionUnavailable(viewModel),
                viewModel.currentScriptProperty(),
                viewModel.selectedScriptSegmentIdProperty());
    }

    private static boolean selectionUnavailable(DocuPodcastShellViewModel viewModel) {
        String selectedSegmentId = viewModel.selectedScriptSegmentIdProperty().get();
        return viewModel.currentScriptProperty().get() == null
                || selectedSegmentId == null
                || selectedSegmentId.isBlank();
    }

    private static BooleanBinding noSelectionBinding(DocuPodcastShellViewModel viewModel) {
        return Bindings.createBooleanBinding(
                () -> noSelection(viewModel),
                viewModel.selectedDocumentBlockIdProperty(),
                viewModel.selectedScriptSegmentIdProperty(),
                viewModel.selectedDocumentTextRangeProperty());
    }

    private static boolean noSelection(DocuPodcastShellViewModel viewModel) {
        String blockId = viewModel.selectedDocumentBlockIdProperty().get();
        String segmentId = viewModel.selectedScriptSegmentIdProperty().get();
        return (blockId == null || blockId.isBlank())
                && (segmentId == null || segmentId.isBlank())
                && viewModel.selectedDocumentTextRangeProperty().get() == null;
    }
}
