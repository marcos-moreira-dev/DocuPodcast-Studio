package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreCanvasActionTooltipsSourceTest {
    @Test
    void canvasButtonsExposeTooltipsAndKeepSelectionBeforeActions() throws Exception {
        String canvas = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreTextSequenceCanvas.java"));

        assertTrue(canvas.contains("private final Tooltip hitTooltip = new Tooltip()"));
        assertTrue(canvas.contains("Tooltip.install(this, hitTooltip)"));
        assertTrue(canvas.contains("setOnMouseMoved(this::updatePointerHint)"));
        assertTrue(canvas.contains("setCursor(hint.isBlank() ? Cursor.DEFAULT : Cursor.HAND)"));
        assertTrue(canvas.contains("Descargar paquete IA de contexto para "));
        assertTrue(canvas.contains("Editar la configuracion de "));
        assertTrue(canvas.contains("Procesar \" + label + \" con el flujo de generacion IA."));
        assertFalse(canvas.contains("mantenerla resaltada"));
        assertSelectionPrecedesAction(canvas, "hit.containsDownload", "aliasFor(hit).ifPresent(contextExportIntervencion)");
        assertSelectionPrecedesAction(canvas, "hit.containsEdit", "aliasFor(hit).ifPresent(this::runPrimaryAction)");
        assertTrue(canvas.contains("private void runPrimaryAction(IntervencionCatalogo.IntervencionInfo alias)"));
    }

    @Test
    void aiNavigatorUsesPrimaryCanvasButtonForProcessingWhenThereIsNoEditor() throws Exception {
        String navigator = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreInterventionNavigator.java"));

        int canvasCreation = navigator.indexOf("new TheatreTextSequenceCanvas(");
        int nullEditor = navigator.indexOf("null,", canvasCreation);
        int processAction = navigator.indexOf("alias -> generateIntervention.accept(scene, alias)", canvasCreation);
        assertTrue(canvasCreation >= 0);
        assertTrue(nullEditor > canvasCreation && nullEditor < processAction);
    }

    private static void assertSelectionPrecedesAction(String source, String branch, String action) {
        int branchIndex = source.indexOf(branch);
        int selectionIndex = source.indexOf("selectHit(hit);", branchIndex);
        int actionIndex = source.indexOf(action, branchIndex);
        assertTrue(branchIndex >= 0);
        assertTrue(selectionIndex > branchIndex && selectionIndex < actionIndex);
    }
}
