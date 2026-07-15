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
import java.util.Objects;
import java.util.function.Supplier;

/** Vertical rail + single active module, inspired by the DMS SideDock. */
public final class WorkspaceSideDock extends BorderPane {
    private static final double DEFAULT_COLLAPSED_MIN_WIDTH = 74.0;
    private static final double DEFAULT_COLLAPSED_PREF_WIDTH = 78.0;
    private static final double DEFAULT_COLLAPSED_MAX_WIDTH = 84.0;
    private static final double FOOTER_RAIL_MIN_WIDTH = 88.0;
    private static final double FOOTER_RAIL_PREF_WIDTH = 92.0;
    private static final double FOOTER_RAIL_MAX_WIDTH = 96.0;

    public enum RailPlacement {
        LEFT,
        RIGHT
    }

    private final SideDockStatePolicy statePolicy = new SideDockStatePolicy();
    private final ReadOnlyBooleanWrapper expanded = new ReadOnlyBooleanWrapper(true);
    private final ReadOnlyObjectWrapper<SideDockModuleId> activeModuleId = new ReadOnlyObjectWrapper<>();
    private final RailPlacement railPlacement;
    private final Supplier<Node> railFooterContent;
    private List<SideDockModule> modules = List.of();
    private SideDockContext context;

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
        getStyleClass().add("workspace-side-dock");
        if (this.railPlacement == RailPlacement.RIGHT) {
            getStyleClass().add("workspace-side-dock-right");
        }
        refresh(context, registry);
        if (initiallyCollapsed) {
            this.activeModuleId.set(null);
            render();
        }
    }

    public void refresh(SideDockContext nextContext, SideDockModuleRegistry registry) {
        this.context = Objects.requireNonNull(nextContext, "nextContext");
        this.modules = Objects.requireNonNull(registry, "registry").modulesFor(nextContext);
        this.activeModuleId.set(statePolicy.choose(activeModuleId.get(), modules));
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
        if (collapsed) {
            collapseActiveModule();
        } else if (activeModuleId.get() == null || modules.stream().noneMatch(module -> module.id() == activeModuleId.get())) {
            activeModuleId.set(statePolicy.choose(null, modules));
            render();
        } else {
            render();
        }
    }

    private void activate(SideDockModuleId moduleId) {
        if (moduleId == activeModuleId.get() && expanded.get()) {
            return;
        }
        this.activeModuleId.set(moduleId);
        render();
    }

    private void collapseActiveModule() {
        this.activeModuleId.set(null);
        render();
    }

    private static AppIcon iconFor(SideDockModule module) {
        String key = ((module == null ? "" : module.iconText()) + " " + (module == null ? "" : module.title()))
                .toLowerCase(java.util.Locale.ROOT);
        if (key.contains("audio")) {
            return AppIcon.AUDIO;
        }
        if (key.contains("imagen")) {
            return AppIcon.IMAGE;
        }
        if (key.contains("video")) {
            return AppIcon.VIDEO;
        }
        if (key.contains("personaje")) {
            return AppIcon.VOICE;
        }
        if (key.contains("mapa")) {
            return AppIcon.STORYBOARD;
        }
        if (key.contains("accion") || key.contains("acción")) {
            return AppIcon.PREPARE;
        }
        if (key.contains("objeto")) {
            return AppIcon.BUNDLE;
        }
        if (key.contains("indice") || key.contains("índice") || key.contains("seccion") || key.contains("sección")) {
            return AppIcon.INDEX_TREE;
        }
        if (key.contains("texto") || key.contains("fragmento")) {
            return AppIcon.TEXT;
        }
        return AppIcon.DEFAULT;
    }

    private void render() {
        VBox rail = new VBox(4);
        getStyleClass().remove("workspace-side-dock-collapsed");
        getStyleClass().remove("workspace-side-dock-expanded");
        rail.getStyleClass().add("side-dock-rail");
        rail.setPadding(new Insets(4));
        rail.setPrefWidth(76);
        rail.setMinWidth(72);
        boolean hasRailFooter = false;
        for (SideDockModule module : modules) {
            Button button = ActionButtonFactory.sideDockRail(iconFor(module), () -> activate(module.id()));
            button.setMinWidth(64);
            button.setPrefWidth(68);
            button.setMaxWidth(68);
            button.setAlignment(Pos.CENTER);
            button.setTooltip(new Tooltip(module.title() + " - " + module.tooltip()));
            button.getStyleClass().add("side-dock-rail-button");
            if (module.id() == activeModuleId.get()) {
                button.getStyleClass().add("side-dock-rail-button-active");
            }
            rail.getChildren().add(button);
        }
        Node footer = railFooterContent.get();
        if (footer != null) {
            hasRailFooter = true;
            detachFromPreviousParent(footer);
            rail.getStyleClass().add("side-dock-rail-with-footer");
            rail.setMinWidth(FOOTER_RAIL_MIN_WIDTH);
            rail.setPrefWidth(FOOTER_RAIL_PREF_WIDTH);
            rail.setMaxWidth(FOOTER_RAIL_MAX_WIDTH);
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
        expanded.set(active != null);
        if (active == null) {
            getStyleClass().add("workspace-side-dock-collapsed");
            setMinWidth(hasRailFooter ? FOOTER_RAIL_MIN_WIDTH : DEFAULT_COLLAPSED_MIN_WIDTH);
            setPrefWidth(hasRailFooter ? FOOTER_RAIL_PREF_WIDTH : DEFAULT_COLLAPSED_PREF_WIDTH);
            setMaxWidth(hasRailFooter ? FOOTER_RAIL_MAX_WIDTH : DEFAULT_COLLAPSED_MAX_WIDTH);
            setCenter(null);
            return;
        }
        getStyleClass().add("workspace-side-dock-expanded");
        setMinWidth(292);
        setPrefWidth(318);
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
        Parent content = active.createView(context);
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
