package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Configuration is reachable from structural chrome, not advertised in Welcome or legacy toolbar. */
final class FrontendConfigurationSurfaceSourceTest {
    @Test
    void configurationIsMenuOnlyOnPrimarySurfaces() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String welcome = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/WelcomeWorkspaceView.java"));
        String toolbar = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/MainToolbarView.java"));
        String ribbon = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonView.java"));

        assertTrue(shell.contains("Menu configuracion = new Menu(\"Configuración\")"));
        assertTrue(shell.contains("commandItem(AppCommandId.OPEN_SETTINGS)"));
        assertTrue(shell.contains(".register(AppCommandId.OPEN_SETTINGS, this::handleOpenSettings)"));
        assertTrue(welcome.contains("Configuración inicial"));
        assertFalse(welcome.contains("openSettings"));
        assertFalse(toolbar.contains("Configuración"));
        assertFalse(toolbar.contains("OPEN_SETTINGS"));
        assertTrue(ribbon.contains("AppCommandId.OPEN_SETTINGS"));
    }
}
