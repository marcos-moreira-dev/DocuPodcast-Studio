package com.marcosmoreiradev.docupodcaststudio.presentation.sidedock;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.AppIcon;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.DocumentSidePanelChrome;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.SidePanelToggleButton;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/** Vertical rail + single active module, inspired by the DMS SideDock. */
public final class WorkspaceSideDock extends BorderPane {
    public static final double COLLAPSED_MIN_WIDTH = 84.0;
    public static final double COLLAPSED_WIDTH = 88.0;
    public static final double COLLAPSED_MAX_WIDTH = 92.0;
    public static final double FOOTER_MIN_WIDTH = 92.0;
    public static final double FOOTER_WIDTH = 96.0;
    public static final double FOOTER_MAX_WIDTH = 100.0;
    public static final double EXPANDED_MIN_WIDTH = 320.0;
    public static final double EXPANDED_PREFERRED_WIDTH = 400.0;

    public enum RailPlacement {
        LEFT,
        RIGHT
    }

    private final SideDockStatePolicy statePolicy = new SideDockStatePolicy();
    private final ReadOnlyBooleanWrapper expanded = new ReadOnlyBooleanWrapper(true);
    private final ReadOnlyObjectWrapper<SideDockModuleId> activeModuleId = new ReadOnlyObjectWrapper<>();
    private final Map<SideDockModuleId, Parent> viewCache = new EnumMap<>(SideDockModuleId.class);
    private final RailPlacement railPlacement;
    private final Supplier<Node> railFooterContent;
    private List<SideDockModule> modules = List.of();
    private SideDockContext context;
    private boolean collapsed;

    public WorkspaceSideDock(SideDockContext context, SideDockModuleRegistry registry) {
        this(context, registry, false);
    }

    public WorkspaceSideDock(SideDockContext context, SideDockModuleRegistry registry, boolean initiallyCollapsed) {
        this(context, registry, initiallyCollapsed, RailPlacement.LEFT);
    }

    public WorkspaceSideDock(
            SideDockContext context,
            SideDockModuleRegistry registry,
            boolean initiallyCollapsed,
            RailPlacement railPlacement) {
        this(context, registry, initiallyCollapsed, railPlacement, null);
    }

    public WorkspaceSideDock(
            SideDockContext context,
            SideDockModuleRegistry registry,
            boolean initiallyCollapsed,
            RailPlacement railPlacement,
            Supplier<Node> railFooterContent) {
        this.railPlacement = Objects.requireNonNull(railPlacement, "railPlacement");
        this.railFooterContent = railFooterContent == null ? () -> null : railFooterContent;
        this.collapsed = initiallyCollapsed;
        getStyleClass().add("workspace-side-dock");
        if (this.railPlacement == RailPlacement.RIGHT) {
            getStyleClass().add("workspace-side-dock-right");
        }
        refresh(context, registry);
    }

    public void refresh(SideDockContext nextContext, SideDockModuleRegistry registry) {
        this.context = Objects.requireNonNull(nextContext, "nextContext");
        this.modules = Objects.requireNonNull(registry, "registry").modulesFor(nextContext);
        this.activeModuleId.set(statePolicy.choose(activeModuleId.get(), modules));
        Set<SideDockModuleId> available = modules.stream().map(SideDockModule::id).collect(java.util.stream.Collectors.toSet());
        viewCache.keySet().removeIf(id -> !available.contains(id));
        render();
    }

    public SideDockModuleId activeModuleId() {
        return activeModuleId.get();
    }

    public ReadOnlyObjectProperty<SideDockModuleId> activeModuleIdProperty() {
        return activeModuleId.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty expandedProperty() {
        return expanded.getReadOnlyProperty();
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
        if (!collapsed && (activeModuleId.get() == null
                || modules.stream().noneMatch(module -> module.id() == activeModuleId.get()))) {
            activeModuleId.set(statePolicy.choose(null, modules));
        }
        render();
    }

    public void activateModule(SideDockModuleId moduleId) {
        if (moduleId == null || modules.stream().noneMatch(module -> module.id() == moduleId)) {
            return;
        }
        activate(moduleId);
    }

    private void activate(SideDockModuleId moduleId) {
        if (moduleId == activeModuleId.get() && !collapsed && expanded.get()) {
            return;
        }
        this.activeModuleId.set(moduleId);
        this.collapsed = false;
        render();
    }

    private void collapseActiveModule() {
        this.collapsed = true;
        render();
    }

    private void render() {
        VBox rail = new VBox(4);
        getStyleClass().remove("workspace-side-dock-collapsed");
        getStyleClass().remove("workspace-side-dock-expanded");
        rail.getStyleClass().add("side-dock-rail");
        rail.setPadding(new Insets(4));
        rail.setPrefWidth(COLLAPSED_WIDTH);
        rail.setMinWidth(COLLAPSED_MIN_WIDTH);
        rail.setMaxWidth(COLLAPSED_MAX_WIDTH);
        for (SideDockModule module : modules) {
            Button button = ActionButtonFactory.sideDockRail(module.icon(), () -> activate(module.id()));
            button.setMinWidth(72);
            button.setPrefWidth(76);
            button.setMaxWidth(80);
            button.setAlignment(Pos.CENTER);
            button.setTooltip(new Tooltip(module.title() + " - " + module.tooltip()));
            button.setAccessibleText(module.title());
            button.getStyleClass().add("side-dock-rail-button");
            // Keep the remembered module internally so it can be restored, but do
            // not advertise a visible selection while the dock itself is closed.
            if (!collapsed && module.id() == activeModuleId.get()) {
                button.getStyleClass().add("side-dock-rail-button-active");
            }
            rail.getChildren().add(button);
        }
        Node footer = railFooterContent.get();
        if (footer != null) {
            detachFromPreviousParent(footer);
            rail.getStyleClass().add("side-dock-rail-with-footer");
            rail.setMinWidth(FOOTER_MIN_WIDTH);
            rail.setPrefWidth(FOOTER_WIDTH);
            rail.setMaxWidth(FOOTER_MAX_WIDTH);
            Region spacer = new Region();
            VBox.setVgrow(spacer, Priority.ALWAYS);
            rail.getChildren().addAll(spacer, footer);
        }
        if (railPlacement == RailPlacement.RIGHT) {
            setLeft(null);
            setRight(rail);
        } else {
            setLeft(rail);
            setRight(null);
        }
        SideDockModule active = modules.stream()
                .filter(module -> module.id() == activeModuleId.get())
                .findFirst()
                .orElse(null);
        expanded.set(active != null && !collapsed);
        if (active == null || collapsed) {
            getStyleClass().add("workspace-side-dock-collapsed");
            setMinWidth(footer == null ? COLLAPSED_MIN_WIDTH : FOOTER_MIN_WIDTH);
            setPrefWidth(footer == null ? COLLAPSED_WIDTH : FOOTER_WIDTH);
            setMaxWidth(footer == null ? COLLAPSED_MAX_WIDTH : FOOTER_MAX_WIDTH);
            setCenter(null);
            return;
        }
        getStyleClass().add("workspace-side-dock-expanded");
        setMinWidth(EXPANDED_MIN_WIDTH);
        setPrefWidth(EXPANDED_PREFERRED_WIDTH);
        setMaxWidth(Double.MAX_VALUE);
        BorderPane frame = new BorderPane();
        frame.getStyleClass().add("side-dock-module-frame");
        Button hide = new SidePanelToggleButton(
                AppIcon.COLLAPSE,
                "Ocultar este panel y dejar solo la barra lateral compacta.",
                this::collapseActiveModule);
        hide.getStyleClass().add("side-dock-hide-button");
        HBox header = DocumentSidePanelChrome.header(active.title(), null, hide).node();
        header.getStyleClass().add("side-dock-module-header");
        frame.setTop(header);
        Parent content = viewCache.computeIfAbsent(active.id(), ignored -> active.createView(context));
        detachFromPreviousParent(content);
        VBox.setVgrow(content, Priority.ALWAYS);
        frame.setCenter(content);
        setCenter(frame);
    }

    private static void detachFromPreviousParent(Node node) {
        Parent parent = node == null ? null : node.getParent();
        if (parent instanceof Pane pane) {
            pane.getChildren().remove(node);
        }
    }
}
