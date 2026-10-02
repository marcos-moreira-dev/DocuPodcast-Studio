package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.admin.AdminWorkspaceLayout;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Compatibility facade over the transversal administrative layout primitives. */
final class TheatreAiWorkspaceLayout {
    private TheatreAiWorkspaceLayout() { }
    static VBox moduleRoot(String title, String eyebrow, String summary) { return AdminWorkspaceLayout.moduleRoot(title, eyebrow, summary); }
    static VBox section(String title) { return AdminWorkspaceLayout.section(title); }
    static HBox masterDetail(Node master, Node detail) { return AdminWorkspaceLayout.masterDetail(master, detail, 320, 420, 520); }
    static HBox balancedMasterDetail(Node master, Node detail) { return AdminWorkspaceLayout.balancedMasterDetail(master, detail); }
    static VBox detailStack(Node... children) { return AdminWorkspaceLayout.detailStack(children); }
    static void detachNode(Node node) { AdminWorkspaceLayout.detachNode(node); }
}
