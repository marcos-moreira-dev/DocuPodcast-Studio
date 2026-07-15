package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ensures theatre demo visuals bind only to the play body, not title/subtitle/notes. */
final class DemoTheatrePlayBodyBindingsHf1SourceTest {
    @Test
    void theatreDemoUsesAnchoredPlayBodyBindingsInOrder() throws Exception {
        String catalog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/examples/ClasspathExampleProjectCatalog.java"));
        String workflow = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ExampleVisualBindingWorkflow.java"));
        String creationWorkflow = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ExampleProjectCreationWorkflow.java"));
        String manifest = Files.readString(Path.of("src/main/resources/examples/aviadores-comicos/PROYECTO_DEMO.md"));

        assertTrue(workflow.contains("segmentByAnchor"));
        assertTrue(workflow.contains("binding.anchorText()"));
        assertTrue(catalog.contains("/examples/aviadores-comicos/PROYECTO_DEMO.md"));
        assertTrue(creationWorkflow.contains("example.hasTheatreMarkdown()"));
        assertTrue(creationWorkflow.contains("configureAviadoresTheatreDemo("));

        String directoryMethod = creationWorkflow.substring(
                creationWorkflow.indexOf("public Result createInDirectory"),
                creationWorkflow.indexOf("public record Result"));
        assertFalse(directoryMethod.contains("importAndBindExampleVisuals"),
                "theatre folder flow must use teatro.md, not the legacy visual binding workflow");

        assertTrue(manifest.contains("texto_inicio=5 | texto_fin=10"));
        assertTrue(manifest.contains("imagen=fragmentos/fragmento_03_revision_capitan.png"));
        assertTrue(manifest.contains("imagen=fragmentos/fragmento_24_cierre_tornillo_dorado.png"));
        assertFalse(manifest.contains("Notas de demo"),
                "demo manifest must not target notes after the play body");
    }
}
