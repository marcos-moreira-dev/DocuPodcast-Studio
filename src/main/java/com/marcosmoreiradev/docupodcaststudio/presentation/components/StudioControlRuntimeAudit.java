package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Runtime complement to the bytecode rule; it audits logical product controls, never skin internals. */
public final class StudioControlRuntimeAudit {
    private static final Set<String> AUDITED_TYPES = Set.of(
            "Button", "ToggleButton", "CheckBox", "RadioButton", "ComboBox", "ColorPicker",
            "TextField", "TextArea", "Spinner", "Slider", "ListView", "TreeView", "TableView",
            "TabPane", "MenuBar", "ProgressBar", "ProgressIndicator", "ScrollPane", "SplitPane",
            "Accordion", "TitledPane");

    private StudioControlRuntimeAudit() { }

    public static List<Violation> audit(Node root) {
        if (root == null) return List.of();
        ArrayList<Violation> violations = new ArrayList<>();
        Set<Node> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        visit(root, root.getClass().getSimpleName(), visited, violations);
        return List.copyOf(violations);
    }

    private static void visit(Node node, String path, Set<Node> visited, List<Violation> violations) {
        if (node == null || !visited.add(node) || isNativeDialogResponse(node)) return;
        if (node instanceof Control control && AUDITED_TYPES.contains(control.getClass().getSimpleName())
                && StudioControlContract.descriptor(control).isEmpty()) {
            violations.add(new Violation(path, control.getClass().getName(), "missing StudioControlDescriptor"));
        }

        if (node instanceof ScrollPane scroll) visit(scroll.getContent(), path + "/content", visited, violations);
        else if (node instanceof SplitPane split) indexed(split.getItems(), path, visited, violations);
        else if (node instanceof TabPane tabs) tabs.getTabs().forEach(tab -> {
            if (!(tab.getProperties().get(StudioControlContract.PROPERTY_KEY) instanceof StudioControlDescriptor)) {
                violations.add(new Violation(path + "/tab[" + tab.getText() + "]", "javafx.scene.control.Tab",
                        "missing StudioControlDescriptor"));
            }
            visit(tab.getContent(), path + "/tab[" + tab.getText() + "]", visited, violations);
        });
        else if (node instanceof TitledPane pane) visit(pane.getContent(), path + "/content", visited, violations);
        else if (node instanceof ToolBar bar) indexed(bar.getItems(), path, visited, violations);
        else if (node instanceof Pane pane) indexed(pane.getChildren(), path, visited, violations);
        else if (!(node instanceof Control) && node instanceof Parent parent) {
            indexed(parent.getChildrenUnmodifiable(), path, visited, violations);
        }
    }

    private static void indexed(List<? extends Node> nodes, String path, Set<Node> visited,
                                List<Violation> violations) {
        for (int index = 0; index < nodes.size(); index++) {
            Node child = nodes.get(index);
            visit(child, path + "/" + child.getClass().getSimpleName() + "[" + index + "]", visited, violations);
        }
    }

    private static boolean isNativeDialogResponse(Node node) {
        for (Node current = node; current != null; current = current.getParent()) {
            if (current.getStyleClass().contains("button-bar")) return true;
        }
        return false;
    }

    public record Violation(String path, String controlType, String reason) { }
}
