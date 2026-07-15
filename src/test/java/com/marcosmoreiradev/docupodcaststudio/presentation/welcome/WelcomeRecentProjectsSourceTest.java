package com.marcosmoreiradev.docupodcaststudio.presentation.welcome;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WelcomeRecentProjectsSourceTest {
    @Test
    void welcomeShowsRecentProjectsAsHyperlinksFromObservableHistory() throws Exception {
        String welcome = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/WelcomeWorkspaceView.java");
        String css = read("src/main/resources/css/welcome.css");

        assertTrue(welcome.contains("ObservableList<RecentProjectEntry> recentProjects"));
        assertTrue(welcome.contains("Consumer<Path> openRecentProject"));
        assertTrue(welcome.contains("new Hyperlink(entry.displayName())"));
        assertTrue(welcome.contains("entry.typeLabel()"));
        assertTrue(welcome.contains("recentProjects.addListener"));
        assertTrue(welcome.contains("openRecentProject.accept(entry.projectFile())"));
        assertTrue(css.contains("welcome-recent-link"));
        assertTrue(css.contains("welcome-recent-type"));
        assertTrue(css.contains("-fx-underline: true"));
    }

    @Test
    void shellPersistsRecentProjectsAndDoesNotLeaveDisabledRecentMenuPlaceholder() throws Exception {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String store = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/RecentProjectsStore.java");

        assertTrue(shell.contains("RecentProjectsStore"));
        assertTrue(shell.contains("recentProjects.setAll(recentProjectsStore.load())"));
        assertTrue(shell.contains("recentProjectsMenu()"));
        assertTrue(shell.contains("handleOpenRecentProject(Path projectFile)"));
        assertTrue(shell.contains("rememberCurrentProject()"));
        assertTrue(shell.contains("recentProjectsStore.remember(projectFile, title, projectType)"));
        assertTrue(shell.contains("handleOpenTechnicalProblemExpress()"));
        assertTrue(shell.contains("TechnicalProblemDialog.showExpress(owner())"));
        assertTrue(store.contains("recent-projects.txt"));
        assertTrue(store.contains("LIMIT = 8"));
        assertTrue(store.contains(".filter(entry -> Files.isRegularFile(entry.projectFile()))"));
        assertTrue(store.contains("persistLimited(existing)"));
        assertTrue(store.contains("persistLimited(ordered.values().stream().limit(LIMIT).toList())"));
        assertFalse(shell.contains("abrirRecientes.setDisable(true)"));
    }

    @Test
    void technicalProblemToolbarUsesDownloadedLucideIconsInsteadOfTemporaryGlyphs() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java");
        String iconView = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/LucideIconView.java");
        String notice = read("src/main/resources/icons/lucide/NOTICE.txt");

        assertTrue(dialog.contains("LucideIconView.of(iconName)"));
        assertTrue(dialog.contains("iconToolButton(\"undo-2\""));
        assertTrue(dialog.contains("iconToolButton(\"trash-2\""));
        assertTrue(dialog.contains("clearInkStrokes"));
        assertTrue(dialog.contains("\"Limpiar solo trazos; conserva imagenes.\""));
        assertTrue(dialog.contains("\"Limpiar lienzo completo; elimina trazos e imagenes.\""));
        assertTrue(dialog.contains("canvasImages.clear()"));
        assertTrue(dialog.contains("setNodeVisible(shrinkImageButton, imageToolsVisible)"));
        assertTrue(dialog.contains("setNodeVisible(copyRegionButton, canvasRegionSelectionMode.get() && hasSelection)"));
        assertTrue(iconView.contains("/icons/lucide/"));
        assertTrue(notice.contains("Lucide icons"));
        assertFalse(dialog.contains("iconToolButton(\"\\\\u21B6\""));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
