package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatrePlaybarLayoutSourceTest {

    @Test
    void theatreScriptDocksPlaybarInLeftSidebarWhenRightDockOpens() throws Exception {
        String workspace = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");
        String theatreDock = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreSideDock.java");
        String sideDock = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/sidedock/WorkspaceSideDock.java");
        String floating = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/FloatingReadingControlBar.java");
        String rail = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/RailReadingControlBar.java");
        String css = read("src/main/resources/css/components/actions.css");

        assertTrue(workspace.contains("RailReadingControlBar"));
        assertTrue(workspace.contains("this::railReadingControl"));
        assertTrue(workspace.contains("railReadingControls.visibleProperty().bind(Bindings.or("));
        assertTrue(workspace.contains("viewModel.documentPlaybarDockedProperty()"));
        assertTrue(workspace.contains("boolean dockedInLeftRail = rightSidebarExpanded || viewModel.documentPlaybarDockedProperty().get()"));
        assertTrue(workspace.contains("controls.setManaged(!dockedInLeftRail)"));
        assertTrue(workspace.contains("theatreSideDock.expandedProperty()"));
        assertTrue(workspace.contains("rightSidebarExpanded.addListener(refresh)"));
        assertTrue(sideDock.contains("Supplier<Node> railFooterContent"));
        assertTrue(sideDock.contains("side-dock-rail-with-footer"));
        assertTrue(sideDock.contains("detachFromPreviousParent"));
        assertTrue(sideDock.contains("FOOTER_RAIL_MIN_WIDTH"));
        assertTrue(sideDock.contains("FOOTER_RAIL_PREF_WIDTH"));
        assertTrue(sideDock.contains("hasRailFooter ? FOOTER_RAIL_PREF_WIDTH"));
        assertFalse(workspace.contains("boolean vertical = theatreScript ? rightSidebarExpanded : narrowReader"));
        assertFalse(workspace.contains("document-left-dock-playback-host"));
        assertTrue(theatreDock.contains("ReadOnlyBooleanProperty expandedProperty()"));
        assertTrue(floating.contains("verticalActionLabel"));
        assertTrue(floating.contains("Reproducir\\nselecci"));
        assertTrue(rail.contains("AppIcon.PAUSE"));
        assertTrue(rail.contains("AppIcon.RESUME"));
        assertTrue(rail.contains("speedButton(\"1x\""));
        assertTrue(rail.contains("speedButton(\"1.75x\""));
        assertTrue(rail.contains("AppIcon.PREVIOUS_FRAGMENT"));
        assertTrue(css.contains("document-rail-playback-controls"));
        assertTrue(css.contains("document-rail-playback-button"));
        assertTrue(css.contains("side-dock-rail-with-footer"));
        assertTrue(css.contains("-fx-wrap-text: true"));
        assertTrue(css.contains("-fx-pref-width: 132px"));
    }

    @Test
    void theatreSideDockStartsWiderAndRemainsExpandable() throws Exception {
        String workspace = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");
        String theatreDock = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreSideDock.java");

        assertTrue(workspace.contains("splitPane.setDividerPositions(0.19, 0.55)"));
        assertTrue(theatreDock.contains("COMPACT_WIDTH = 700.0"));
        assertTrue(theatreDock.contains("EXPANDED_WIDTH = 980.0"));
        assertTrue(theatreDock.contains("COMPACT_MIN_WIDTH = 520.0"));
        assertTrue(theatreDock.contains("EXPANDED_MIN_WIDTH = 680.0"));
        assertTrue(theatreDock.contains("setMaxWidth(Double.MAX_VALUE)"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
