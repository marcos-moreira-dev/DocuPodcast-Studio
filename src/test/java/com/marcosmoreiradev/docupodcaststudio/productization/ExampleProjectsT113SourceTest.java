package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExampleProjectsT113SourceTest {
    @Test
    void t113AddsExamplesMenuDialogCatalogAndDemoResources() throws Exception {
        String commandIds = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandId.java");
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String catalog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/examples/ClasspathExampleProjectCatalog.java");
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/examples/ExampleProjectDialog.java");
        String welcome = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/WelcomeWorkspaceView.java");

        assertTrue(commandIds.contains("OPEN_EXAMPLE_PROJECT"));
        assertTrue(shell.contains("Menu ejemplos = new Menu(\"Ejemplos\")"));
        assertTrue(shell.contains("handleOpenExampleProject"));
        assertTrue(shell.contains("ExampleProjectCreationWorkflow") || shell.contains("createExampleProject()"));
        assertTrue(shell.contains("DirectoryChooser"));
        assertTrue(shell.contains("createTheatreExampleProjectFromDescriptor"));
        assertTrue(shell.contains("createInDirectory"));
        String creationWorkflow = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ExampleProjectCreationWorkflow.java");
        assertTrue(creationWorkflow.contains("projectFileExistedBefore"));
        assertTrue(creationWorkflow.contains("!projectFileExistedBefore"));
        assertTrue(dialog.contains("SectionHeader"));
        assertTrue(dialog.contains("ActionButtonFactory.primary"));
        assertTrue(welcome.contains("Probar ejemplo"));
        assertTrue(catalog.contains("instinto-creativo"));
        assertTrue(catalog.contains("caso-contable-cafe-luna"));
        assertTrue(catalog.contains("aviadores-comicos"));
        assertTrue(catalog.contains("Demo teatral desde manifiesto MD"));
        assertTrue(catalog.contains("/examples/aviadores-comicos/PROYECTO_DEMO.md"));
        assertTrue(Files.exists(Path.of("src/main/resources/examples/instinto-creativo/source.docx")));
        assertTrue(Files.exists(Path.of("src/main/resources/examples/caso-contable-cafe-luna/source.docx")));
        assertTrue(Files.exists(Path.of("src/main/resources/examples/aviadores-comicos/source.docx")));
        assertTrue(Files.exists(Path.of("src/main/resources/examples/aviadores-comicos/PROYECTO_DEMO.md")));
        assertTrue(Files.exists(Path.of("src/main/resources/examples/aviadores-comicos/assets/fragmentos/fragmento_09_mapa_norte_abajo.png")));
        assertTrue(Files.exists(Path.of("src/main/resources/examples/aviadores-comicos/assets/fragmentos/fragmento_24_cierre_tornillo_dorado.png")));
        assertTrue(Files.exists(Path.of("src/main/resources/examples/aviadores-comicos/assets/personajes/capitan_bigote/capitan_bigote_01_frontal.png")));
        assertTrue(Files.exists(Path.of("src/main/resources/examples/aviadores-comicos/assets/utileria/obj_mapa_01.png")));
    }

    @Test
    void t113DocumentsGuidedDemoProjectFlowAndRemainingRoadmap() throws Exception {
        String doc = read("docs/productizacion/T113_EJEMPLOS_PROYECTOS_DEMO.md");
        String validation = read("VALIDATION.md");
        String handoff = read("AI_HANDOFF.md");
        String registry = read("docs/productizacion/REGISTRO_TANDAS_DOCUPODCAST.md");

        assertTrue(doc.contains("ventana secundaria"));
        assertTrue(doc.contains("FileChooser"));
        assertTrue(doc.contains("DirectoryChooser"));
        assertTrue(doc.contains("teatro.md"));
        assertTrue(doc.contains("source/source.docx"));
        assertTrue(doc.contains("Instinto Creativo"));
        assertTrue(doc.contains("Cafe Luna Azul") || doc.contains("Café Luna Azul"));
        assertTrue(doc.contains("El vuelo del Tornillo Dorado"));
        assertTrue(doc.contains("DEMO-TEATRO-RC1") || doc.contains("se asocian automaticamente"));
        assertTrue(validation.contains("Validacion T113"));
        assertTrue(handoff.contains("Base vigente: T113"));
        assertTrue(registry.contains("T113 — Menú Ejemplos"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
