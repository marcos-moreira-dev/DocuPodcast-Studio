package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProjectSourcePortabilityT126ASourceTest {
    @Test
    void materializationRebasesCurrentDocumentToProjectSourceCopy() throws Exception {
        String materialized = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/MaterializedImportedDocument.java");
        assertTrue(materialized.contains("ReadableDocument projectSourceDocument"),
                "La materialización debe devolver la fuente canónica copiada dentro del proyecto.");

        String repository = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/document/ReadableDocumentWorkspaceRepository.java");
        assertTrue(repository.contains("rebaseToProjectSource"),
                "El repositorio debe rebasar el documento activo a source/<archivo>.");
        assertTrue(repository.contains("toJson(projectSourceDocument"),
                "document/document.json debe reflejar la copia canónica del proyecto.");
        assertFalse(repository.contains("Files.copy(document.sourcePath(), copiedSource"),
                "No debe copiar a ciegas sin normalizar ni detectar copia en la misma ruta.");
    }

    @Test
    void saveWorkflowKeepsSessionAndUiOnProjectSourceCopy() throws Exception {
        String workflow = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ProjectWorkflowCoordinator.java");
        assertTrue(workflow.contains("materializeWithResult"));
        assertTrue(workflow.contains("hydrateImportedDocument(materializedDocument.projectSourceDocument())"));

        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        assertTrue(viewModel.contains("flatMap(ProjectSession::importedDocument).ifPresent"));
        assertTrue(viewModel.contains("Fuente canónica"));
    }

    @Test
    void userGetsClearNoticeAboutProjectSourceCopy() throws Exception {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/dialogs/ProjectSourceCopyNoticeDialog.java");
        assertTrue(dialog.contains("Documento fuente guardado en el proyecto"));
        assertTrue(dialog.contains("Refrescar contenido leerá esa copia"));
        assertTrue(dialog.contains("No volver a mostrar este aviso"));
        assertTrue(view.contains("hideProjectSourceCopyNotice"));
        assertTrue(view.contains("ProjectSourceCopyNoticeDialog"));
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
