package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** HF10E guardrails: chunks can start from the status bar and DOCX tables use a transversal component. */
final class DocUxHf10EChunksAndStyledTableSourceTest {
    @Test
    void statusBarChunkButtonPreparesNarrationBeforeSubmittingAudio() throws Exception {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String status = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/status/StatusBarView.java");

        assertTrue(shell.contains("viewModel.generateAudioChunksWithoutPlayback()"));
        assertTrue(viewModel.contains("generateAudioChunksWithoutPlayback"));
        assertTrue(viewModel.contains("buildNarrationScriptFromDocument()"));
        assertTrue(viewModel.contains("submitAudioGeneration()"));
        assertTrue(status.contains("Reconstruir fragmentos de audio"));
        assertTrue(status.contains("Renderizar desde aquí"));
        assertTrue(status.contains("Mostrar detalles de generación"));
    }

    @Test
    void docxTablesUseTransversalStyledGridComponent() throws Exception {
        String component = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/SourceTableGridView.java");
        String visual = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/SourceVisualBlockView.java");
        String css = read("src/main/resources/css/components/source-visual.css");
        String docs = read("DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md");

        assertTrue(component.contains("final class SourceTableGridView"));
        assertTrue(visual.contains("new SourceTableGridView"));
        assertTrue(css.contains("DOC-TABLE-HF11B"));
        assertTrue(css.contains("#4F46E5"));
        assertTrue(css.contains("ui-source-table-header-cell"));
        assertTrue(docs.contains("DOC-TABLE-HF11B"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
