package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guardrail for T123-CAT01: Markdown is a source document, not a user-facing script import/export path. */
final class MarkdownAsDocumentT123Cat01SourceTest {
    @Test
    void markdownNormalFlowIsDocumentSourceOnly() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String documentImporter = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/document/MarkdownDocumentImporter.java"));

        assertTrue(shell.contains("Markdown (*.md, *.markdown)"));
        assertTrue(documentImporter.contains("read-only source documents"));
        assertFalse(shell.contains("handleImportScriptMarkdown"));
        assertFalse(shell.contains("handleExportScriptMarkdown"));
        assertFalse(viewModel.contains("importNarrationScriptMarkdown(Path"));
        assertFalse(viewModel.contains("exportNarrationScriptMarkdown(Path"));
    }

    @Test
    void productResourcesDoNotExposeLegacyScriptMarkdownContract() throws Exception {
        String catalog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/resources/OfficialAiResourceCatalog.java"));
        String exporter = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/resources/ClasspathAiResourceExporter.java"));
        String capabilities = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/workspace/WorkspaceCapability.java"));
        String services = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/ScriptApplicationServices.java"))
                + Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/ExportApplicationServices.java"));

        assertFalse(catalog.contains("docupodcast-script-v1"));
        assertFalse(exporter.contains("docupodcast-script-v1"));
        assertFalse(capabilities.contains("IMPORT_SCRIPT_MARKDOWN"));
        assertFalse(services.contains("ImportNarrationScriptMarkdownUseCase"));
        assertFalse(services.contains("ExportNarrationScriptMarkdownUseCase"));
    }

    @Test
    void bundleUsesPreparedReadingEvidenceInsteadOfNarrationMarkdown() throws Exception {
        String exporter = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/export/FileSystemProjectBundleExporter.java"));
        String readiness = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/export/InspectExportReadinessUseCase.java"));
        String artifactKind = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/export/ExportableArtifactKind.java"));

        assertTrue(exporter.contains("lectura_preparada.md"));
        assertFalse(exporter.contains("guion_narrable.md"));
        assertFalse(readiness.contains("NARRATION_MARKDOWN"));
        assertFalse(artifactKind.contains("NARRATION_MARKDOWN"));
    }
}
