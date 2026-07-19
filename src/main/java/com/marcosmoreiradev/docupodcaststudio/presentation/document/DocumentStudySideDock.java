package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import com.marcosmoreiradev.docupodcaststudio.presentation.sidedock.SideDockContext;
import com.marcosmoreiradev.docupodcaststudio.presentation.sidedock.SideDockModuleId;
import com.marcosmoreiradev.docupodcaststudio.presentation.sidedock.SideDockModuleRegistry;
import com.marcosmoreiradev.docupodcaststudio.presentation.sidedock.StaticSideDockModule;
import com.marcosmoreiradev.docupodcaststudio.presentation.sidedock.WorkspaceSideDock;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;

import java.util.List;
import java.util.function.Supplier;

/** Right dock for document-study tools. Mirrors the theatre dock shell without theatre modules. */
public final class DocumentStudySideDock extends BorderPane {
    private static final double COLLAPSED_WIDTH = 84.0;
    private static final double EXPANDED_WIDTH = 720.0;
    private static final double EXPANDED_MIN_WIDTH = 560.0;

    private final WorkspaceSideDock dock;
    private final SideDockContext dockContext;
    private final SideDockModuleRegistry dockRegistry;
    private boolean dockExpanded;

    public DocumentStudySideDock(
            DocuPodcastShellViewModel viewModel,
            ObservableSet<String> selection,
            ObservableList<PdfRegionCaptureDraft> pdfRegionSelection,
            Supplier<List<DocumentBlock>> selectedBlocks,
            Supplier<List<PdfRegionCaptureDraft>> selectedPdfRegions,
            Runnable clearSelection,
            Runnable generateProblem) {
        getStyleClass().add("document-study-side-dock");
        this.dockContext = new SideDockContext(WorkspaceKind.DOCUMENT_READER, "Estudio documental");
        this.dockRegistry = registry(viewModel, selection, pdfRegionSelection, selectedBlocks,
                selectedPdfRegions, clearSelection, generateProblem);
        this.dock = new WorkspaceSideDock(
                dockContext,
                dockRegistry,
                false,
                WorkspaceSideDock.RailPlacement.RIGHT);
        dockExpanded = viewModel.documentRightRailVisibleProperty().get();
        dock.setCollapsed(!dockExpanded);
        applyDockWidth();
        viewModel.documentRightRailVisibleProperty().addListener((obs, oldValue, visible) ->
                dock.setCollapsed(!Boolean.TRUE.equals(visible)));
        dock.expandedProperty().addListener((obs, oldValue, visible) -> {
            dockExpanded = Boolean.TRUE.equals(visible);
            applyDockWidth();
            if (viewModel.documentRightRailVisibleProperty().get() != dockExpanded) {
                viewModel.documentRightRailVisibleProperty().set(dockExpanded);
            }
        });
        viewModel.currentDocumentProperty().addListener((obs, oldValue, newValue) ->
                dock.refresh(dockContext, dockRegistry));
        viewModel.currentProjectModeProperty().addListener((obs, oldValue, newValue) ->
                dock.refresh(dockContext, dockRegistry));
        setCenter(dock);
    }

    public ReadOnlyBooleanProperty expandedProperty() {
        return dock.expandedProperty();
    }

    private void applyDockWidth() {
        if (!dockExpanded) {
            setMinWidth(COLLAPSED_WIDTH);
            setPrefWidth(COLLAPSED_WIDTH);
            setMaxWidth(COLLAPSED_WIDTH);
            return;
        }
        setMinWidth(EXPANDED_MIN_WIDTH);
        setPrefWidth(EXPANDED_WIDTH);
        setMaxWidth(Double.MAX_VALUE);
    }

    private SideDockModuleRegistry registry(
            DocuPodcastShellViewModel viewModel,
            ObservableSet<String> selection,
            ObservableList<PdfRegionCaptureDraft> pdfRegionSelection,
            Supplier<List<DocumentBlock>> selectedBlocks,
            Supplier<List<PdfRegionCaptureDraft>> selectedPdfRegions,
            Runnable clearSelection,
            Runnable generateProblem) {
        return new SideDockModuleRegistry()
                .register(new StaticSideDockModule(
                        SideDockModuleId.DOCUMENT_STUDY_VIDEO,
                        "Contenido del video",
                        "Edita cada parrafo, tabla y diapositiva final del video documental.",
                        "Video",
                        () -> new DocumentStudyVideoPanel(viewModel),
                        context -> viewModel.documentaryVideoConfigurationAvailable()))
                .register(StaticSideDockModule.of(
                        SideDockModuleId.DOCUMENT_TECHNICAL_PROBLEM,
                        "Problema",
                        "Selecciona bloques del documento para preparar y resolver un problema tecnico.",
                        "Problema",
                        () -> scrollableProblemPanel(new DocumentTechnicalProblemPanel(
                                viewModel,
                                selection,
                                pdfRegionSelection,
                                selectedBlocks,
                                selectedPdfRegions,
                                clearSelection,
                                generateProblem))));
    }

    private Parent scrollableProblemPanel(DocumentTechnicalProblemPanel panel) {
        panel.setMinWidth(0);
        panel.setMaxWidth(Double.MAX_VALUE);
        ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMinWidth(0);
        scroll.setMaxWidth(Double.MAX_VALUE);
        scroll.getStyleClass().add("document-technical-problem-scroll");
        return scroll;
    }
}
