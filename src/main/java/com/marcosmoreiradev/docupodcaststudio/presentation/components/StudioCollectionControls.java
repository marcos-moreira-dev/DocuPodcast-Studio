package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

/** Official constructors for collection controls. */
public final class StudioCollectionControls {
    private StudioCollectionControls() { }

    public static <T> ListView<T> listView() { return listView(null); }
    public static <T> ListView<T> listView(ObservableList<T> items) {
        ListView<T> control = items == null ? new ListView<>() : new ListView<>(items);
        control.getStyleClass().add("ui-collection-list");
        return StudioControlContract.mark(control, StudioControlFamily.COLLECTION,
                StudioControlVariant.REGULAR, StudioControlDensity.REGULAR);
    }

    public static <T> TreeView<T> treeView() { return treeView(null); }
    public static <T> TreeView<T> treeView(TreeItem<T> root) {
        TreeView<T> control = root == null ? new TreeView<>() : new TreeView<>(root);
        control.getStyleClass().add("ui-collection-tree");
        return StudioControlContract.mark(control, StudioControlFamily.COLLECTION,
                StudioControlVariant.NAVIGATION, StudioControlDensity.REGULAR);
    }

    public static <T> TableView<T> tableView() {
        TableView<T> control = new TableView<>();
        control.getStyleClass().add("ui-collection-table");
        return StudioControlContract.mark(control, StudioControlFamily.COLLECTION,
                StudioControlVariant.REGULAR, StudioControlDensity.REGULAR);
    }
}
