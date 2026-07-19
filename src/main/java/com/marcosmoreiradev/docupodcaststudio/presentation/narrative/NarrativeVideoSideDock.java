package com.marcosmoreiradev.docupodcaststudio.presentation.narrative;

import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import com.marcosmoreiradev.docupodcaststudio.presentation.sidedock.SideDockContext;
import com.marcosmoreiradev.docupodcaststudio.presentation.sidedock.SideDockModuleId;
import com.marcosmoreiradev.docupodcaststudio.presentation.sidedock.SideDockModuleRegistry;
import com.marcosmoreiradev.docupodcaststudio.presentation.sidedock.StaticSideDockModule;
import com.marcosmoreiradev.docupodcaststudio.presentation.sidedock.WorkspaceSideDock;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.scene.layout.BorderPane;

import java.util.Objects;

/** Right-side production dock used only by narrative-video projects. */
public final class NarrativeVideoSideDock extends BorderPane {
    private static final double COLLAPSED_WIDTH = 84.0;
    private static final double EXPANDED_WIDTH = 780.0;
    private static final double EXPANDED_MIN_WIDTH = 600.0;

    private final DocuPodcastShellViewModel viewModel;
    private final SideDockContext context =
            new SideDockContext(WorkspaceKind.DOCUMENT_READER, "Video narrativo");
    private final SideDockModuleRegistry registry;
    private final WorkspaceSideDock dock;
    private boolean expanded;

    public NarrativeVideoSideDock(DocuPodcastShellViewModel viewModel) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
        getStyleClass().add("narrative-video-side-dock");
        registry = new SideDockModuleRegistry().register(new StaticSideDockModule(
                SideDockModuleId.NARRATIVE_VIDEO_CONTENT,
                "Contenido del video",
                "Genera y revisa las tomas del video narrativo por parrafo.",
                "Video",
                () -> new NarrativeVideoPanel(viewModel),
                ignored -> viewModel.narrativeVideoConfigurationAvailable()));
        dock = new WorkspaceSideDock(context, registry, false, WorkspaceSideDock.RailPlacement.RIGHT);
        expanded = viewModel.documentRightRailVisibleProperty().get();
        dock.setCollapsed(!expanded);
        applyWidth();
        viewModel.documentRightRailVisibleProperty().addListener((obs, oldValue, visible) ->
                dock.setCollapsed(!Boolean.TRUE.equals(visible)));
        dock.expandedProperty().addListener((obs, oldValue, visible) -> {
            expanded = Boolean.TRUE.equals(visible);
            applyWidth();
            if (viewModel.documentRightRailVisibleProperty().get() != expanded) {
                viewModel.documentRightRailVisibleProperty().set(expanded);
            }
        });
        viewModel.currentDocumentProperty().addListener((obs, oldValue, newValue) -> refresh());
        viewModel.currentProjectModeProperty().addListener((obs, oldValue, newValue) -> refresh());
        setCenter(dock);
    }

    public ReadOnlyBooleanProperty expandedProperty() {
        return dock.expandedProperty();
    }

    private void refresh() {
        dock.refresh(context, registry);
    }

    private void applyWidth() {
        if (!expanded) {
            setMinWidth(COLLAPSED_WIDTH);
            setPrefWidth(COLLAPSED_WIDTH);
            setMaxWidth(COLLAPSED_WIDTH);
            return;
        }
        setMinWidth(EXPANDED_MIN_WIDTH);
        setPrefWidth(EXPANDED_WIDTH);
        setMaxWidth(Double.MAX_VALUE);
    }
}
