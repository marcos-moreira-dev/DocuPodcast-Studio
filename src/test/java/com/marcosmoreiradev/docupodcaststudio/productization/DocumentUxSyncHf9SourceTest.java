package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentUxSyncHf9SourceTest {
    @Test
    void sentenceSelectionDoesNotHighlightWholeParagraphWhenRangeIsActive() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        assertTrue(source.contains("boolean sentenceRangeActive = viewModel.selectedDocumentTextRangeProperty().get() != null"),
                "La seleccion por oracion debe bloquear el borde de seleccion del bloque completo.");
        assertTrue(source.contains("document-sentence-selected"),
                "La UI debe mantener resaltado visual de la oracion seleccionada.");
    }

    @Test
    void playbackCursorSelectsTheCurrentSentenceUnit() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        assertTrue(source.contains("syncActivePlaybackCue"),
                "El seguimiento de lectura debe sincronizar cue de playback, no solo bloque.");
        assertTrue(source.contains("sentenceForCueUnit"),
                "El cue unitario debe seleccionar la oracion correspondiente en la hoja e inspectores.");
        assertTrue(source.contains("lastIndexOf(\"-U\")"),
                "Los ids SEG-xxx-Uyyy deben resolverse como indice de oracion dentro del bloque.");
    }

    @Test
    void visualRailListsAllScenesWithoutEightItemCap() throws Exception {
        String rail = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentMediaRailView.java"));
        assertFalse(rail.contains("Math.min(8"),
                "El rail visual no debe ocultar fragmentos con un limite fijo de ocho items.");
        assertFalse(rail.contains("fragmentos más"),
                "El rail visual debe listar todos los fragmentos en el scroll, no esconderlos en un contador.");
    }

    @Test
    void visualRailRebuildsPresentationFromNarrativeImageLayers() throws Exception {
        String vm = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        assertTrue(vm.contains("buildStoryboardFromImageLayers().build("),
                "Las tarjetas visuales deben reflejar las capas de imagen actuales aunque el storyboard cacheado quede viejo.");
        assertTrue(vm.contains("effectiveStoryboard.scenesFor(script)"),
                "La proyeccion visual debe usar el storyboard efectivo reconstruido desde capas.");
    }
}
