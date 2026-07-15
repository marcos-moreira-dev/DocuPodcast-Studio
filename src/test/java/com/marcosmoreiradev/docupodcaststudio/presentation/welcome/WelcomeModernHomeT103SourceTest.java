package com.marcosmoreiradev.docupodcaststudio.presentation.welcome;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T103A guardrail: Inicio is a modern desktop start page, not a web landing page. */
final class WelcomeModernHomeT103SourceTest {
    @Test
    void welcomeUsesDesktopStartCompositionWithoutLandingPage() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/WelcomeWorkspaceView.java"));
        String css = Files.readString(Path.of("src/main/resources/css/welcome.css"));

        assertTrue(source.contains("welcome-desktop-home"));
        assertTrue(source.contains("desktopGrid()"));
        assertTrue(source.contains("startActions()"));
        assertTrue(source.contains("centerIntro()"));
        assertTrue(source.contains("recentColumn()"));
        assertTrue(source.contains("new Circle("));
        assertFalse(source.contains("new Polygon("));
        assertFalse(source.contains("welcome-bottom-wave"));
        assertTrue(source.contains("watermark.setTranslateX(104)"));
        assertTrue(source.contains("watermark.setTranslateY(76)"));
        assertFalse(source.contains("welcome-product-landing"));
        assertFalse(source.contains("welcome-hero-card"));
        assertFalse(source.contains("productPreview()"));
        assertTrue(css.contains("desktop app start page"));
        assertTrue(css.contains("welcome-desktop-grid"));
        assertFalse(css.contains("welcome-bottom-wave"));
        assertFalse(css.contains("welcome-product-preview"));
    }

    @Test
    void welcomeKeepsRealActionsAndProductPromise() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/WelcomeWorkspaceView.java"));

        assertTrue(source.contains("ActionButtonFactory.secondary(title, action)"));
        assertTrue(source.contains("Abrir documento"));
        assertTrue(source.contains("Abrir proyecto"));
        assertTrue(source.contains("Nuevo proyecto"));
        assertTrue(source.contains("Configuración inicial"));
        assertTrue(source.contains("Escucha rápido"));
        assertTrue(source.contains("Abre el documento"));
        assertTrue(source.contains("Escucha y estudia"));
        assertTrue(source.contains("Documento protegido"));
        assertTrue(source.contains("Sin proyectos recientes"));
        assertFalse(source.contains("new InfoBadge(\"Local\""));
        assertFalse(source.contains("Fuente intacta"));
        assertFalse(source.contains("V1: lector + capas"));
        assertFalse(source.contains("Consejo: usa la barra superior"));
        assertFalse(source.contains("new Button("));
    }

    @Test
    void welcomeDoesNotReintroduceTechnicalOrUnimplementedSurfaces() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/WelcomeWorkspaceView.java"));

        assertTrue(source.contains("OPEN_SETTINGS"));
        assertTrue(source.contains("initialSetup"));
        assertFalse(source.contains("openSettings"));
        assertFalse(source.contains("Whisper"));
        assertFalse(source.contains("STT"));
        assertFalse(source.contains("Jobs"));
        assertFalse(source.contains("manifest"));
        assertFalse(source.contains("SCRIPT_EDITOR"));
        assertFalse(source.contains("AUDIO_JOBS"));
        assertFalse(source.contains("STORYBOARD"));
    }
}
