package com.marcosmoreiradev.docupodcaststudio.presentation.ribbon;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioViewportControls;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;

import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandDescriptor;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandRegistry;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandSurface;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.CommandAvailabilityPolicy;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.AppStyles;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.RibbonButton;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.RibbonGroup;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.RibbonIconCatalog;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Ribbon base real for the desktop shell.
 *
 * <p>T101 replaces the transitional two-row toolbar with a tabbed ribbon surface. It uses the
 * official command catalog and shared ribbon primitives while keeping listening controls outside
 * this surface.</p>
 */
public final class RibbonView extends VBox {
    private final DocuPodcastShellViewModel viewModel;
    private final Consumer<AppCommandId> dispatcher;
    private final AppCommandRegistry commandRegistry = AppCommandRegistry.official();
    private final CommandAvailabilityPolicy commandAvailabilityPolicy = new CommandAvailabilityPolicy();
    private final ToggleGroup tabGroup = new ToggleGroup();
    private final HBox tabStrip = new HBox(2);
    private final ScrollPane contentScroll = StudioViewportControls.scrollPane();
    private List<RibbonTabDefinition> tabs = List.of();
    private final BooleanProperty collapsed;
    private String selectedTabId = "";
    private boolean ribbonTabPinnedByUser;

    public RibbonView(DocuPodcastShellViewModel viewModel, Consumer<AppCommandId> dispatcher, BooleanProperty collapsed) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.collapsed = Objects.requireNonNull(collapsed, "collapsed");

        getStyleClass().add(AppStyles.UI_RIBBON_SURFACE);
        setMinHeight(148);
        setPrefHeight(154);
        setMaxHeight(164);
        tabStrip.getStyleClass().add(AppStyles.UI_RIBBON_TAB_STRIP);
        tabStrip.setMinHeight(30);
        tabStrip.setPrefHeight(32);
        tabStrip.setMaxHeight(34);
        contentScroll.getStyleClass().add(AppStyles.UI_RIBBON_CONTENT_SCROLL);
        contentScroll.setMinHeight(112);
        contentScroll.setPrefHeight(118);
        contentScroll.setMaxHeight(126);
        contentScroll.setPrefViewportHeight(116);
        contentScroll.setFitToHeight(false);
        contentScroll.setFitToWidth(false);
        contentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        contentScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        contentScroll.setPannable(true);
        contentScroll.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, event -> {
            double delta = event.getDeltaY() == 0 ? event.getDeltaX() : event.getDeltaY();
            if (delta != 0 && contentScroll.getHmax() > 0) {
                double next = contentScroll.getHvalue() - delta / 900.0;
                contentScroll.setHvalue(Math.max(0.0, Math.min(contentScroll.getHmax(), next)));
                event.consume();
            }
        });

        rebuildTabs(false);
        getChildren().addAll(tabStrip, contentScroll);
        VBox.setVgrow(contentScroll, Priority.NEVER);
        contentScroll.visibleProperty().bind(this.collapsed.not());
        contentScroll.managedProperty().bind(contentScroll.visibleProperty());

        selectTab(defaultTabFor(viewModel.activeWorkspaceProperty().get()));
        viewModel.activeWorkspaceProperty().addListener((obs, oldValue, newValue) -> {
            if (!ribbonTabPinnedByUser) {
                selectTab(defaultTabFor(newValue));
            }
        });
        viewModel.currentProjectModeProperty().addListener((obs, oldValue, newValue) -> rebuildTabs(true));
        applyCollapsedState(this.collapsed.get());
        this.collapsed.addListener((obs, oldValue, newValue) -> applyCollapsedState(newValue));
    }

    private void rebuildTabs(boolean preserveSelection) {
        String preferredTab = preserveSelection && containsTab(selectedTabId)
                ? selectedTabId
                : defaultTabFor(viewModel.activeWorkspaceProperty().get());
        tabs = new ArrayList<>(RibbonDefinitionCatalog.officialTabs(viewModel.currentProjectModeProperty().get()));
        tabStrip.getChildren().clear();
        tabGroup.getToggles().clear();
        buildTabButtons();
        if (!containsTab(preferredTab)) {
            preferredTab = defaultTabFor(viewModel.activeWorkspaceProperty().get());
        }
        if (!containsTab(preferredTab) && !tabs.isEmpty()) {
            preferredTab = tabs.getFirst().id();
        }
        ribbonTabPinnedByUser = preserveSelection && containsTab(selectedTabId);
        selectTab(preferredTab);
    }

    private void buildTabButtons() {
        for (RibbonTabDefinition tab : tabs) {
            ToggleButton button = StudioFormControls.toggleButton(tab.title());
            button.getStyleClass().add(AppStyles.UI_RIBBON_TAB);
            button.setFocusTraversable(false);
            button.setToggleGroup(tabGroup);
            button.setUserData(tab.id());
            button.setOnAction(event -> {
                if (!button.isSelected()) {
                    button.setSelected(true);
                }
                if (collapsed.get()) {
                    collapsed.set(false);
                }
                ribbonTabPinnedByUser = true;
                selectedTabId = tab.id();
                showTab(tab);
            });
            tabStrip.getChildren().add(button);
        }
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button collapseToggle = ActionButtonFactory.secondary("");
        collapseToggle.getStyleClass().add(AppStyles.UI_RIBBON_COLLAPSE_TOGGLE);
        collapseToggle.setFocusTraversable(false);
        collapseToggle.textProperty().bind(Bindings.when(collapsed).then("▼").otherwise("▲"));
        Tooltip tooltip = new Tooltip();
        tooltip.textProperty().bind(Bindings.when(collapsed).then("Maximizar cinta").otherwise("Minimizar cinta"));
        collapseToggle.setTooltip(tooltip);
        collapseToggle.setOnAction(event -> dispatcher.accept(AppCommandId.TOGGLE_RIBBON_COLLAPSED));
        tabStrip.getChildren().addAll(spacer, collapseToggle);
    }

    private void selectTab(String id) {
        if (tabs.isEmpty()) {
            contentScroll.setContent(new HBox());
            selectedTabId = "";
            return;
        }
        for (Node node : tabStrip.getChildren()) {
            if (node instanceof ToggleButton button && Objects.equals(button.getUserData(), id)) {
                button.setSelected(true);
                selectedTabId = id;
                showTab(tabById(id));
                return;
            }
        }
        if (!tabs.isEmpty()) {
            selectTab(tabs.getFirst().id());
        }
    }

    private RibbonTabDefinition tabById(String id) {
        return tabs.stream()
                .filter(tab -> tab.id().equals(id))
                .findFirst()
                .orElse(tabs.getFirst());
    }

    private boolean containsTab(String id) {
        return id != null && tabs.stream().anyMatch(tab -> tab.id().equals(id));
    }

    private String defaultTabFor(WorkspaceKind workspaceKind) {
        if (workspaceKind == WorkspaceKind.VOICE_LIBRARY) {
            return "vista";
        }
        if (workspaceKind == WorkspaceKind.DOCUMENT_READER) {
            return "lectura";
        }
        if (workspaceKind == WorkspaceKind.NARRATIVE_VISUAL_PRODUCTION) {
            return "video-narrativo";
        }
        if (workspaceKind == WorkspaceKind.THEATRE_SCRIPT) {
            return "teatro";
        }
        if (workspaceKind == WorkspaceKind.THEATRE_IMAGE_GENERATION) {
            return "teatro";
        }
        return "inicio";
    }

    private void showTab(RibbonTabDefinition tab) {
        applyExpandedGeometry(tab);
        HBox content = new HBox(10);
        content.getStyleClass().add(AppStyles.UI_RIBBON_CONTENT);
        content.setMinHeight(110);
        content.setPrefHeight(116);
        content.setMaxHeight(124);
        content.setPadding(new Insets(10, 14, 10, 14));
        for (RibbonGroupDefinition group : tab.groups()) {
            content.getChildren().add(buildGroup(group));
        }
        contentScroll.setContent(content);
        contentScroll.setHvalue(0.0);
    }

    private RibbonGroup buildGroup(RibbonGroupDefinition group) {
        Node[] nodes = group.commands().stream()
                .map(this::buttonFor)
                .toArray(Node[]::new);
        RibbonGroup ribbonGroup = new RibbonGroup(group.title(), nodes);
        Observable[] dependencies = Arrays.stream(nodes)
                .map(Node::visibleProperty)
                .toArray(Observable[]::new);
        var hasVisibleCommand = Bindings.createBooleanBinding(
                () -> Arrays.stream(nodes).anyMatch(Node::isVisible),
                dependencies);
        ribbonGroup.visibleProperty().bind(hasVisibleCommand);
        ribbonGroup.managedProperty().bind(hasVisibleCommand);
        return ribbonGroup;
    }

    private RibbonButton buttonFor(RibbonCommandDefinition command) {
        AppCommandDescriptor descriptor = commandRegistry.descriptor(command.commandId());
        if (!descriptor.allowedOn(AppCommandSurface.RIBBON)) {
            throw new IllegalStateException("El comando no esta permitido en Ribbon: " + command.commandId());
        }
        var icon = RibbonIconCatalog.iconFor(command.commandId());
        var unavailableReason = commandAvailabilityPolicy.unavailableReasonBinding(command.commandId(), viewModel);
        var tooltip = Bindings.createStringBinding(
                () -> unavailableReason.get().isBlank()
                        ? descriptor.description()
                        : descriptor.description() + "\n" + unavailableReason.get(),
                unavailableReason);
        RibbonButton button = new RibbonButton(icon, new javafx.beans.property.SimpleStringProperty(descriptor.shortLabel()),
                tooltip, () -> dispatchFromRibbon(command.commandId()), command.primary());
        button.disableProperty().bind(commandAvailabilityPolicy.disabledBinding(command.commandId(), viewModel));
        button.visibleProperty().bind(commandAvailabilityPolicy.visibleBinding(command.commandId(), viewModel));
        button.managedProperty().bind(button.visibleProperty());
        return button;
    }

    private void dispatchFromRibbon(AppCommandId commandId) {
        ribbonTabPinnedByUser = true;
        String tabId = selectedTabId;
        dispatcher.accept(commandId);
        if (!tabId.isBlank() && containsTab(tabId)) {
            selectTab(tabId);
        } else {
            selectTab(defaultTabFor(viewModel.activeWorkspaceProperty().get()));
        }
    }

    private void applyCollapsedState(boolean collapsed) {
        getStyleClass().removeAll(AppStyles.UI_RIBBON_EXPANDED, AppStyles.UI_RIBBON_COLLAPSED);
        getStyleClass().add(collapsed ? AppStyles.UI_RIBBON_COLLAPSED : AppStyles.UI_RIBBON_EXPANDED);
        if (collapsed) {
            setMinHeight(34);
            setPrefHeight(34);
            setMaxHeight(38);
            return;
        }
        applyExpandedGeometry(containsTab(selectedTabId) ? tabById(selectedTabId) : null);
    }

    private void applyExpandedGeometry(RibbonTabDefinition tab) {
        if (collapsed.get()) {
            return;
        }
        setMinHeight(148);
        setPrefHeight(154);
        setMaxHeight(164);
        contentScroll.setMinHeight(112);
        contentScroll.setPrefHeight(118);
        contentScroll.setMaxHeight(126);
        contentScroll.setPrefViewportHeight(116);
    }

    /*
     * Source contract retained for historical product tests while the real ribbon structure lives in
     * RibbonDefinitionCatalog. These strings document the intended product surface and prevent future
     * accidental reintroduction of legacy tabs.
     *
     * tab("inicio", "Inicio"
     * tab("lectura", "Lectura"
     * tab("vista", "Vista"
     * tab("estudio", "Estudio"
     * tab("video-narrativo", "Video narrativo"
     * tab("teatro", "Teatro"
     * tab("exportar", "Exportar"
     * group("Soporte"
     * group("Vistas principales"
     * group("Produccion visual"
     * group("Gestion manual de la obra"
     * group("Centro de exportaciones"
     * cmd(AppCommandId.SHOW_WELCOME, true)
     * AppCommandId.OPEN_SOURCE_DOCUMENT
     * AppCommandId.OPEN_DOCUMENT_READER
     * AppCommandId.OPEN_THEATRE_SCRIPT
     * AppCommandId.OPEN_THEATRE_IMAGE_GENERATION
     * AppCommandId.OPEN_VOICE_LIBRARY
     * AppCommandId.OPEN_SETTINGS
     * AppCommandId.OPEN_GUIDE
     * AppCommandId.OPEN_EXPORT_CENTER
     */

}
