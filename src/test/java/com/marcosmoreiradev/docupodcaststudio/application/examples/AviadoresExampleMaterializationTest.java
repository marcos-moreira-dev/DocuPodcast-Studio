package com.marcosmoreiradev.docupodcaststudio.application.examples;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AviadoresExampleMaterializationTest {
    @TempDir
    Path tempDir;

    @Test
    void materializesDocxMarkdownAndAssetsForAviadoresDemo() throws Exception {
        ExampleProjectDescriptor example = new ClasspathExampleProjectCatalog().listExamples().stream()
                .filter(candidate -> candidate.id().equals("aviadores-comicos"))
                .findFirst()
                .orElseThrow();

        Path selectedFolder = tempDir.resolve("Archivos del libro");
        Path demoFolder = selectedFolder.resolve("Aviadores Comicos Demo");
        ExampleProjectMaterialization materialized = new CreateExampleProjectUseCase()
                .materializeInDirectory(example, selectedFolder);

        assertTrue(Files.exists(demoFolder));
        assertTrue(Files.notExists(selectedFolder.resolve("teatro.md")));
        assertTrue(Files.notExists(selectedFolder.resolve("source")));
        assertTrue(Files.notExists(selectedFolder.resolve("assets")));
        assertTrue(Files.exists(materialized.sourceDocument()));
        assertEquals(demoFolder.resolve(Path.of("source", "source.docx")).normalize(), materialized.sourceDocument());
        assertTrue(Files.exists(materialized.theatreMarkdownFile()));
        assertEquals(demoFolder.resolve("teatro.md").normalize(), materialized.theatreMarkdownFile());
        assertEquals(demoFolder.resolve("Aviadores Comicos Demo.docupodcast.json").normalize(), materialized.projectFile());
        assertTrue(Files.notExists(materialized.projectFile()));
        assertTrue(Files.readString(materialized.theatreMarkdownFile()).contains("texto_inicio=5 | texto_fin=10"));
        assertTrue(Files.readString(materialized.theatreMarkdownFile()).contains("mapa_espacial=mapas/mapa-espacial.png"));
        assertEquals(example.assets().size(), materialized.visualAssets().size());
        assertTrue(materialized.visualAssets().stream()
                .anyMatch(path -> path.endsWith(Path.of("assets", "fragmentos", "fragmento_09_mapa_norte_abajo.png"))));
        assertTrue(materialized.visualAssets().stream()
                .anyMatch(path -> path.endsWith(Path.of("assets", "mapas", "mapa-espacial.png"))));
        assertTrue(materialized.visualAssets().stream()
                .anyMatch(path -> path.endsWith(Path.of("assets", "personajes", "narrador", "narrador_01_frontal.png"))));
    }
}
