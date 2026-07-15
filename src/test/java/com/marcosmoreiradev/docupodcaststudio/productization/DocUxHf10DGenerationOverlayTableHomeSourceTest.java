package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** DOC-UX-HF10D/HF11/HF12: chunk generation details, table grid and Welcome navigation from Vista. */
final class DocUxHf10DGenerationOverlayTableHomeSourceTest {
    @Test
    void statusBarGenerationButtonOpensOverlayAndRenamesHiddenDetails() throws Exception {
        String status = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/status/StatusBarView.java"));
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String overlay = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/process/LongProcessOverlayView.java"));

        assertTrue(status.contains("Reconstruir fragmentos de audio"));
        assertTrue(status.contains("Renderizar desde aquí"));
        assertTrue(status.contains("processOverlayExpanded.set(true)"));
        assertTrue(status.contains("Mostrar detalles de generación"));
        assertTrue(status.contains("Seguir generando"));
        assertTrue(shell.contains("handleGenerateChunksFromStatusBar"));
        assertTrue(overlay.contains("Generando fragmentos de audio"));
        assertTrue(overlay.contains("la generación continuará"));
    }

    @Test
    void docxTablesRenderAsSoberGridInsteadOfRawMarkdownPlaceholder() throws Exception {
        String visual = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/SourceVisualBlockView.java"));
        String document = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String css = Files.readString(Path.of("src/main/resources/css/components/source-visual.css"));

        assertTrue(visual.contains("tableGrid"));
        assertTrue(visual.contains("GridPane"));
        assertTrue(document.contains("sourceVisualHeadlineOrSentenceFlow"));
        assertTrue(document.contains("SourceVisualBlockView.tableGrid"));
        assertTrue(css.contains("ui-source-table-grid"));
        assertTrue(css.contains("ui-source-table-header-cell"));
    }

    @Test
    void vistaRibbonCanReturnToWelcomeHome() throws Exception {
        String registry = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java"));
        String ribbon = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonView.java"));
        String icons = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/RibbonIconCatalog.java"));

        assertTrue(registry.contains("SHOW_WELCOME"));
        assertTrue(registry.contains("AppCommandSurface.RIBBON"));
        assertTrue(ribbon.contains("cmd(AppCommandId.SHOW_WELCOME, true)"));
        assertTrue(icons.contains("case SHOW_WELCOME"));
    }

    @Test
    void currentDocumentationRegistersTheNewTandas() throws Exception {
        String register = Files.readString(Path.of("DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md"));
        String roadmap = Files.readString(Path.of("DOCUMENTACION_ACTUAL/03_ROADMAP_PENDIENTE_CIERRE.md"));

        assertTrue(register.contains("DOC-UX-HF10D"));
        assertTrue(register.contains("DOC-TABLE-HF11"));
        assertTrue(register.contains("VIEW-HOME-HF12"));
        assertTrue(roadmap.contains("PLAYBACK-HF9"));
    }
}
