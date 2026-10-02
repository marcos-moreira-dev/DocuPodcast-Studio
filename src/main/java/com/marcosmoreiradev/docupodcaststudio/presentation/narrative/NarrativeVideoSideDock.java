package com.marcosmoreiradev.docupodcaststudio.presentation.narrative;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.AppIcon;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import com.marcosmoreiradev.docupodcaststudio.presentation.sidedock.SideDockContext;
import com.marcosmoreiradev.docupodcaststudio.presentation.sidedock.SideDockModuleId;
import com.marcosmoreiradev.docupodcaststudio.presentation.sidedock.SideDockModuleRegistry;
import com.marcosmoreiradev.docupodcaststudio.presentation.sidedock.SideDockHost;
import com.marcosmoreiradev.docupodcaststudio.presentation.sidedock.SideDockLayoutPolicy;
import com.marcosmoreiradev.docupodcaststudio.presentation.sidedock.StaticSideDockModule;
import com.marcosmoreiradev.docupodcaststudio.presentation.sidedock.WorkspaceSideDock;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.scene.layout.BorderPane;

import java.util.Objects;

/** Right-side production dock used only by narrative-video projects. */
public final class NarrativeVideoSideDock extends BorderPane {
    private final DocuPodcastShellViewModel viewModel;
    private final SideDockContext context =
            new SideDockContext(WorkspaceKind.DOCUMENT_READER, "Video narrativo");
    private final SideDockModuleRegistry registry;
    private final SideDockHost dock;

    public NarrativeVideoSideDock(DocuPodcastShellViewModel viewModel) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
        registry = new SideDockModuleRegistry().register(new StaticSideDockModule(
                SideDockModuleId.NARRATIVE_VIDEO_CONTENT,
                "Contenido del video",
                "Genera y revisa las tomas del video narrativo por parrafo.",
                AppIcon.PRODUCT_DOCUMENTARY_VIDEO_CONTENT,
                () -> new NarrativeVideoPanel(viewModel),
                ignored -> viewModel.narrativeVideoConfigurationAvailable()));
        dock = new SideDockHost(
                "narrative-video-side-dock",
                context,
                registry,
                viewModel.documentRightRailVisibleProperty(),
                SideDockLayoutPolicy.standard(600.0, 780.0),
                WorkspaceSideDock.RailPlacement.RIGHT);
        viewModel.currentDocumentProperty().addListener((obs, oldValue, newValue) -> refresh());
        viewModel.currentProjectModeProperty().addListener((obs, oldValue, newValue) -> refresh());
        dock.installInto(this);
    }

    public ReadOnlyBooleanProperty expandedProperty() {
        return dock.expandedProperty();
    }

    private void refresh() {
        dock.refresh(context, registry);
    }

}
