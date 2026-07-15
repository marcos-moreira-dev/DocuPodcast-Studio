package com.marcosmoreiradev.docupodcaststudio.presentation.script;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MarkdownImportUiSourceTest {
    @Test
    void markdownIsHandledOnlyAsDocumentInTheNormalMenu() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String capabilities = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/workspace/WorkspaceCapability.java"));

        assertFalse(capabilities.contains("IMPORT_SCRIPT_MARKDOWN"));
        assertFalse(viewModel.contains("importNarrationScriptMarkdown(Path sourceFile)"));
        assertTrue(shell.contains("commandItem(AppCommandId.OPEN_SOURCE_DOCUMENT)"));
        assertFalse(shell.contains("new MenuItem(\"Importar guion Markdown"));
        assertFalse(shell.contains("new MenuItem(\"Importar narración Markdown"));
    }
}
