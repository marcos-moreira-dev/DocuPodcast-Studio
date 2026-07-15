package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocUxHf9AHotfixSourceTest {
    @Test
    void visualRailLambdaUsesFinalStoryboardReference() throws Exception {
        String vm = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        assertTrue(vm.contains("final StoryboardDocument storyboardForImageLayers = storyboard"),
                "La proyeccion visual reconstruida no debe capturar una variable local reasignada dentro de una lambda Java.");
        assertTrue(vm.contains("script, storyboardForImageLayers, session.project().assets()"),
                "La lambda debe usar una referencia final/effectively final para compilar en Maven.");
    }

    @Test
    void refreshPersistsSourceVisualMetadataWithoutForcingAudioReset() throws Exception {
        String coordinator = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/SourceDocumentRefreshCoordinator.java"));
        String vm = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        assertTrue(coordinator.contains("sourceVisualMetadataChanged(currentDocument, classified)"),
                "Refrescar fuente debe detectar imagenes/tablas rehidratadas aunque el texto narrable no cambie.");
        assertTrue(coordinator.contains("before.sourceVisual() || after.sourceVisual()"),
                "La deteccion debe limitarse a bloques visuales fuente, no a cualquier metadata accidental.");
        assertTrue(vm.contains("Visuales internos del Word actualizados; guarda el proyecto para conservarlos."),
                "La UI debe explicar que los visuales internos se actualizaron sin decir falsamente que el texto cambio.");
    }
}
