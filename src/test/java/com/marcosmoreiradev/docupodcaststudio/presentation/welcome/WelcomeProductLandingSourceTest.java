package com.marcosmoreiradev.docupodcaststudio.presentation.welcome;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T103A guardrail: the initial screen is a polished desktop start page, not a technical index. */
final class WelcomeProductLandingSourceTest {
    @Test
    void welcomeUsesSharedStyledComponentsForRealActions() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/WelcomeWorkspaceView.java"));

        assertTrue(source.contains("ActionButtonFactory.secondary(title, action)"));
        assertTrue(source.contains("commandRow("));
        assertTrue(source.contains("welcome-command-row"));
        assertFalse(source.contains("new Button("), "La bienvenida no debe hardcodear botones JavaFX nativos.");
        assertFalse(source.contains("welcome-product-landing"), "Inicio no debe volver al formato landing page.");
    }

    @Test
    void welcomeSpeaksAboutProductFlowNotConfigurationOrInternalWorkspaces() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/WelcomeWorkspaceView.java"));

        assertTrue(source.contains("Escucha rápido"));
        assertTrue(source.contains("Abre el documento"));
        assertTrue(source.contains("Escucha y estudia"));
        assertTrue(source.contains("OPEN_SETTINGS"));
        assertTrue(source.contains("Configuración inicial"));
        assertFalse(source.contains("Narración avanzada"));
        assertFalse(source.contains("Jobs"));
        assertFalse(source.contains("manifest"));
        assertFalse(source.contains("docupodcast-script-v1"));
    }
}
