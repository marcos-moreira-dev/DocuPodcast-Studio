package com.marcosmoreiradev.docupodcaststudio.presentation.shell;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guardrail: menu bar follows the current product navigation, not legacy technical workspaces. */
final class MainMenuNavigationSourceTest {
    @Test
    void menuBarUsesCurrentProductMenusAndHidesTechnicalModules() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String menuBlock = shell.substring(shell.indexOf("private MenuBar buildMenuBar()"), shell.indexOf("public void handleOpenProjectFolder()"));

        assertTrue(menuBlock.contains("new Menu(\"Archivo\")"));
        assertTrue(menuBlock.contains("new Menu(\"Proyecto\")"));
        assertTrue(menuBlock.contains("new Menu(\"Fuente documental\")"));
        assertTrue(menuBlock.contains("new Menu(\"Ver\")"));
        assertTrue(menuBlock.contains("new Menu(\"Lectura\")"));
        assertTrue(menuBlock.contains("new Menu(\"Exportar\")"));
        assertTrue(menuBlock.contains("new Menu(\"Configuración\")"));
        assertTrue(menuBlock.contains("new Menu(\"Ayuda\")"));

        assertFalse(menuBlock.contains("new Menu(\"Editar\")"),
                "Editar ya no debe reaparecer como menú vacío/legacy hasta tener acciones reales.");
        assertFalse(menuBlock.contains("new Menu(\"Documento\")"),
                "La superficie actual distingue Fuente documental del documento narrable.");
        assertFalse(menuBlock.contains("new Menu(\"Herramientas\")"),
                "Herramientas era un contenedor técnico; debe permanecer fuera del frente normal.");
        assertFalse(menuBlock.contains("new Menu(\"Narración\")"));
        assertFalse(menuBlock.contains("new Menu(\"Voz\")"));
        assertFalse(menuBlock.contains("new Menu(\"Storyboard\")"));
        assertFalse(menuBlock.contains("new Menu(\"Audio\")"));
        assertFalse(menuBlock.contains("new Menu(\"Reproducción\")"));

        assertTrue(menuBlock.contains("commandItem(AppCommandId.OPEN_PROJECT_FOLDER)"));
        assertTrue(menuBlock.contains("commandItem(AppCommandId.TOGGLE_FULLSCREEN)"));
        assertTrue(menuBlock.contains("commandItem(AppCommandId.OPEN_SOURCE_DOCUMENT)"));
    }

    @Test
    void normalMenuDoesNotExposeMarkdownNarrationOrDiagnosticExport() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        int menuStart = shell.indexOf("private MenuBar buildMenuBar()");
        int menuEnd = shell.indexOf("public void handleOpenProjectFolder()");
        String menuBlock = shell.substring(menuStart, menuEnd);

        assertFalse(menuBlock.contains("Importar narración Markdown"));
        assertFalse(menuBlock.contains("Exportar reporte diagnóstico"));
        assertFalse(menuBlock.contains("Exportar narración Markdown"));
        assertFalse(menuBlock.contains("Ver estado de exportación"));
        assertTrue(menuBlock.contains("Fuente documental"));
    }
}
