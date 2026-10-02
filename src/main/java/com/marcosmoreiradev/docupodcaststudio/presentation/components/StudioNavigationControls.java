package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/** Official navigation and popup controls. */
public final class StudioNavigationControls {
    private StudioNavigationControls() { }

    public static TabPane tabPane(Tab... tabs) {
        TabPane control = new TabPane(tabs == null ? new Tab[0] : tabs);
        control.getStyleClass().add("ui-navigation-tabs");
        return StudioControlContract.mark(control, StudioControlFamily.NAVIGATION,
                StudioControlVariant.NAVIGATION, StudioControlDensity.REGULAR);
    }

    public static Tab tab() { return tab("", null); }
    public static Tab tab(String text) { return tab(text, null); }
    public static Tab tab(String text, Node content) {
        Tab tab = content == null ? new Tab(text) : new Tab(text, content);
        tab.getProperties().put(StudioControlContract.PROPERTY_KEY,
                new StudioControlDescriptor(StudioControlFamily.NAVIGATION, StudioControlVariant.NAVIGATION,
                        StudioControlDensity.REGULAR, text, ""));
        return tab;
    }

    public static MenuBar menuBar(Menu... menus) {
        MenuBar control = new MenuBar(menus == null ? new Menu[0] : menus);
        control.getStyleClass().add("ui-navigation-menu-bar");
        return StudioControlContract.mark(control, StudioControlFamily.NAVIGATION,
                StudioControlVariant.NAVIGATION, StudioControlDensity.COMPACT);
    }

    public static ContextMenu contextMenu(MenuItem... items) {
        ContextMenu control = new ContextMenu(items == null ? new MenuItem[0] : items);
        control.getStyleClass().add("ui-navigation-context-menu");
        control.getProperties().put(StudioControlContract.PROPERTY_KEY,
                new StudioControlDescriptor(StudioControlFamily.NAVIGATION,
                        StudioControlVariant.NAVIGATION, StudioControlDensity.COMPACT,
                        "Menu contextual", ""));
        return control;
    }
}
