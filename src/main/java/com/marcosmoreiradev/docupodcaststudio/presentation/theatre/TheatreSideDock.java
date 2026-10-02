package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.AppIcon;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.CollapsibleModuleSplitPane;
import com.marcosmoreiradev.docupodcaststudio.presentation.document.DocumentImageContextPanel;
import com.marcosmoreiradev.docupodcaststudio.presentation.document.DocumentMediaRailView;
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
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;

import java.util.function.BooleanSupplier;

/** Right dock for theatre/script modules that complement the shared document reader. */
public final class TheatreSideDock extends BorderPane {
    private final SideDockHost dock;
    private final IntervencionBoundaryStore sceneBoundaryStore;

    public TheatreSideDock(DocuPodcastShellViewModel viewModel, BooleanSupplier saveProjectRequest) {
        this(viewModel, saveProjectRequest, new IntervencionBoundaryStore());
    }

    public TheatreSideDock(DocuPodcastShellViewModel viewModel, BooleanSupplier saveProjectRequest,
                           IntervencionBoundaryStore sceneBoundaryStore) {
        this.sceneBoundaryStore = sceneBoundaryStore == null ? new IntervencionBoundaryStore() : sceneBoundaryStore;
        this.dock = new SideDockHost(
                "theatre-side-dock",
                new SideDockContext(WorkspaceKind.THEATRE_SCRIPT, "Teatro"),
                registry(viewModel, saveProjectRequest),
                viewModel.documentRightRailVisibleProperty(),
                SideDockLayoutPolicy.standardWithCompact(680.0, 980.0, 520.0, 700.0),
                WorkspaceSideDock.RailPlacement.RIGHT,
                "theatre-side-dock-compact",
                null);
        dock.activeModuleIdProperty().addListener((obs, oldValue, moduleId) -> {
            if (moduleId != SideDockModuleId.THEATRE_FRAGMENT_IMAGES
                    && moduleId != SideDockModuleId.THEATRE_CHARACTERS
                    && moduleId != SideDockModuleId.THEATRE_OBJECTS) {
                dock.setCompact(false);
            }
        });
        dock.installInto(this);
    }

    public ReadOnlyBooleanProperty expandedProperty() {
        return dock.expandedProperty();
    }

    private SideDockModuleRegistry registry(DocuPodcastShellViewModel viewModel, BooleanSupplier saveProjectRequest) {
        return new SideDockModuleRegistry()
                .register(StaticSideDockModule.of(
                        SideDockModuleId.THEATRE_FRAGMENT_IMAGES,
                        "Capas multimedia",
                        "Imagenes y pistas de audio asignadas a las intervenciones.",
                        AppIcon.PRODUCT_THEATRE_MEDIA_LAYERS,
                        () -> multimediaLayers(viewModel, saveProjectRequest)))
                .register(StaticSideDockModule.of(
                        SideDockModuleId.THEATRE_CHARACTERS,
                        "Personajes",
                        "Personajes detectados y referencias visuales por personaje.",
                        AppIcon.PRODUCT_THEATRE_CHARACTERS,
                        () -> characters(viewModel)))
                .register(StaticSideDockModule.of(
                        SideDockModuleId.THEATRE_TEXTUAL_MAP,
                        "Mapa textual",
                        "Intervenciones 1, 2, 3... como identificadores de fragmentos.",
                        AppIcon.PRODUCT_THEATRE_TEXT_MAP,
                        () -> new TheatreTextualMapPanel(viewModel, sceneBoundaryStore)))
                .register(StaticSideDockModule.of(
                        SideDockModuleId.THEATRE_SPATIAL_MAP,
                        "Mapa espacial y acciones",
                        "Escenarios, posiciones y desplazamientos por fragmento.",
                        AppIcon.PRODUCT_THEATRE_SPATIAL_ACTIONS,
                        () -> new TheatreSpatialActionMapPanel(viewModel, sceneBoundaryStore)))
                // THEATRE_ACTIONS is kept as a legacy id but is now fused into THEATRE_SPATIAL_MAP.
                .register(StaticSideDockModule.of(
                        SideDockModuleId.THEATRE_OBJECTS,
                        "Objetos",
                        "Utileria, escenografia y referencias visuales de escena.",
                        AppIcon.PRODUCT_THEATRE_OBJECTS,
                        () -> objects(viewModel)));
    }

    private Parent multimediaLayers(DocuPodcastShellViewModel viewModel, BooleanSupplier saveProjectRequest) {
        DocumentImageContextPanel assignment = new DocumentImageContextPanel(viewModel, saveProjectRequest);
        CollapsibleModuleSplitPane[] splitHolder = new CollapsibleModuleSplitPane[1];
        DocumentMediaRailView rail = new DocumentMediaRailView(viewModel, () -> {
            if (splitHolder[0] != null) {
                splitHolder[0].secondaryVisibleProperty().set(false);
            }
        });
        assignment.getStyleClass().add("theatre-fragment-image-assignment");
        rail.getStyleClass().add("theatre-fragment-image-rail");
        allowFlexibleWidth(assignment);
        allowFlexibleWidth(rail);

        CollapsibleModuleSplitPane split = new CollapsibleModuleSplitPane(
                "Asignar imagen",
                assignment,
                "Acciones visuales",
                rail,
                0.45,
                false);
        splitHolder[0] = split;
        split.getStyleClass().add("theatre-fragment-images-split");
        allowFlexibleWidth(split);
        TheatreAudioTrackWorkspace audio = new TheatreAudioTrackWorkspace(viewModel);
        allowFlexibleWidth(audio);
        TheatreMultimediaLayersPane multimedia = new TheatreMultimediaLayersPane(
                split, split::showPrimary, audio, audio::showEditor);
        allowFlexibleWidth(multimedia);
        return multimedia;
    }

    private Parent characters(DocuPodcastShellViewModel viewModel) {
        TheatreCharactersPanel panel = new TheatreCharactersPanel(viewModel);
        bindCompactContent(panel.primaryVisibleProperty(), panel.secondaryVisibleProperty());
        allowFlexibleWidth(panel);
        return panel;
    }

    private Parent objects(DocuPodcastShellViewModel viewModel) {
        TheatreObjectsPanel panel = new TheatreObjectsPanel(viewModel);
        bindCompactContent(panel.primaryVisibleProperty(), panel.secondaryVisibleProperty());
        allowFlexibleWidth(panel);
        return panel;
    }

    private void bindCompactContent(
            ReadOnlyBooleanProperty primaryVisible,
            ReadOnlyBooleanProperty secondaryVisible) {
        primaryVisible.addListener((obs, oldValue, visible) ->
                dock.setCompact(!primaryVisible.get() || !secondaryVisible.get()));
        secondaryVisible.addListener((obs, oldValue, visible) ->
                dock.setCompact(!primaryVisible.get() || !secondaryVisible.get()));
    }

    private static void allowFlexibleWidth(Parent parent) {
        if (parent instanceof Region region) {
            region.setMinWidth(0);
            region.setMaxWidth(Double.MAX_VALUE);
        }
    }
}
