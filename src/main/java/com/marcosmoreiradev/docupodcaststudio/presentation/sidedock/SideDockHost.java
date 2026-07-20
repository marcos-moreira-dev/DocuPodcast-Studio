package com.marcosmoreiradev.docupodcaststudio.presentation.sidedock;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Transversal shell that synchronizes expansion, persisted preference, widths,
 * rail placement and compact mode for product-specific side-docks.
 */
public class SideDockHost extends BorderPane {
    private final WorkspaceSideDock dock;
    private final BooleanProperty expandedPreference;
    private final SideDockLayoutPolicy layoutPolicy;
    private final String compactStyleClass;
    private boolean compact;

    public SideDockHost(String styleClass,
                        SideDockContext context,
                        SideDockModuleRegistry registry,
                        BooleanProperty expandedPreference,
                        SideDockLayoutPolicy layoutPolicy,
                        WorkspaceSideDock.RailPlacement railPlacement) {
        this(styleClass, context, registry, expandedPreference, layoutPolicy, railPlacement, null, null);
    }

    public SideDockHost(String styleClass,
                        SideDockContext context,
                        SideDockModuleRegistry registry,
                        BooleanProperty expandedPreference,
                        SideDockLayoutPolicy layoutPolicy,
                        WorkspaceSideDock.RailPlacement railPlacement,
                        String compactStyleClass,
                        Supplier<Node> railFooterContent) {
        this.expandedPreference = Objects.requireNonNull(expandedPreference, "expandedPreference");
        this.layoutPolicy = Objects.requireNonNull(layoutPolicy, "layoutPolicy");
        this.compactStyleClass = compactStyleClass == null ? "" : compactStyleClass.strip();
        if (styleClass != null && !styleClass.isBlank()) getStyleClass().add(styleClass.strip());
        this.dock = new WorkspaceSideDock(
                Objects.requireNonNull(context, "context"),
                Objects.requireNonNull(registry, "registry"),
                !expandedPreference.get(),
                Objects.requireNonNull(railPlacement, "railPlacement"),
                railFooterContent);
        expandedPreference.addListener((obs, oldValue, visible) ->
                dock.setCollapsed(!Boolean.TRUE.equals(visible)));
        dock.expandedProperty().addListener((obs, oldValue, visible) -> {
            boolean expanded = Boolean.TRUE.equals(visible);
            applyWidth(expanded);
            if (expandedPreference.get() != expanded) expandedPreference.set(expanded);
        });
        setCenter(dock);
        applyWidth(dock.expandedProperty().get());
    }

    public final ReadOnlyBooleanProperty expandedProperty() { return dock.expandedProperty(); }
    public final ReadOnlyObjectProperty<SideDockModuleId> activeModuleIdProperty() {
        return dock.activeModuleIdProperty();
    }

    public final void refresh(SideDockContext context, SideDockModuleRegistry registry) {
        dock.refresh(context, registry);
    }

    public final void setCompact(boolean compact) {
        if (this.compact == compact) return;
        this.compact = compact;
        if (!compactStyleClass.isBlank()) {
            if (compact && !getStyleClass().contains(compactStyleClass)) getStyleClass().add(compactStyleClass);
            else if (!compact) getStyleClass().remove(compactStyleClass);
        }
        applyWidth(dock.expandedProperty().get());
    }

    public final boolean isCompact() { return compact; }

    private void applyWidth(boolean expanded) {
        SideDockLayoutPolicy.WidthRange width = !expanded
                ? layoutPolicy.collapsed()
                : compact ? layoutPolicy.compactExpanded() : layoutPolicy.expanded();
        setMinWidth(width.min());
        setPrefWidth(width.pref());
        setMaxWidth(width.max());
    }
}
